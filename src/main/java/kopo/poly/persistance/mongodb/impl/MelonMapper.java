package kopo.poly.persistance.mongodb.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import kopo.poly.dto.MelonDTO;
import kopo.poly.persistance.mongodb.AbstractMongoDBComon;
import kopo.poly.persistance.mongodb.IMelonMapper;
import kopo.poly.util.CmmUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.mongodb.client.model.Updates.set;

@Slf4j
@Component
@RequiredArgsConstructor
public class MelonMapper extends AbstractMongoDBComon implements IMelonMapper {

    private final MongoTemplate mongodb;

    @Override
    public int insertSong(List<MelonDTO> pList, String colNm) throws Exception {

        log.info(this.getClass().getName() + ".insertSong Start!");

        int res = 0;

        // 데이터를 저장할 컬렉션 생성
        super.createCollection(mongodb, colNm, "collectTime");

        // 저장할 컬렉션 객체 생성
        MongoCollection<Document> col = mongodb.getCollection(colNm);

        for (MelonDTO pDTO : pList) {
            col.insertOne(new Document(new ObjectMapper().convertValue(pDTO, Map.class))); // 레코드 한개씩 저장하기
        }

        res = 1;

        log.info(this.getClass().getName() + ".insertSong End!");

        return res;
    }

    @Override
    public List<MelonDTO> getSongList(String colNm) throws Exception {

        log.info(this.getClass().getName() + ".getSongList Start!");

        // 조회 결과를 전달하기 위한 객체 생성하기
        List<MelonDTO> rList = new LinkedList<>();

        MongoCollection<Document> col = mongodb.getCollection(colNm);

        // 조회 결과 중 출력할 컬럼들(SQL의 SELECT절과 FROM절 가운데 컬럼들과 유사함)
        Document projection = new Document();
        projection.append("song", "$song");
        projection.append("singer", "$singer");

        // MongoDB는 무조건 ObjectId가 자동생성되며, ObjectID는 사용하지 않을때, 조회할 필요가 없음
        // ObjectId를 가지고 오지 않을 때 사용함
        projection.append("_id", 0);

        // MongoDB의 find 명령어를 통해 조회할 경우 사용함
        // 조회하는 데이터의 양이 적은 경우, find를 사용하고, 데이터양이 많은 경우 무조건 Aggregate 사용한다.
        FindIterable<Document> rs = col.find(new Document()).projection(projection);

        for (Document doc : rs) {
            String song = CmmUtil.nvl(doc.getString("song"));
            String singer = CmmUtil.nvl(doc.getString("singer"));

            log.info("song : " + song + "/ singer : " + singer);

            MelonDTO rDTO = MelonDTO.builder().song(song).singer(singer).build();

            // 레코드 결과를 List에 저장하기
            rList.add(rDTO);

        }
        log.info(this.getClass().getName() + ".getSongList End!");

        return rList;
    }

    @Override
    public List<MelonDTO> getSingerSongCnt(String colNm) throws Exception {

        log.info(this.getClass().getName() + ".getSingerSongCnt Start!");

        // 조회 결과를 전달하기 위한 객체 생성하기
        List<MelonDTO> rList = new LinkedList<>();

        // MongoDB 조회 쿼리
        List<? extends Bson> pipeline = Arrays.asList(
                new Document().append("$group",
                        new Document().append("_id", new Document().append("singer", "$singer")).append("COUNT(singer)",
                                new Document().append("$sum", 1))),
                new Document()
                        .append("$project",
                                new Document().append("singer", "$_id.singer").append("singerCnt", "$COUNT(singer)")
                                        .append("_id", 0)),
                new Document().append("$sort", new Document().append("singerCnt", -1)));

        MongoCollection<Document> col = mongodb.getCollection(colNm);
        AggregateIterable<Document> rs = col.aggregate(pipeline).allowDiskUse(true);

        for (Document doc : rs) {
            String singer = doc.getString("singer");
            int singerCnt = doc.getInteger("singerCnt", 0);

            log.info("singer : " + singer + "/ singerCnt : " + singerCnt);

            MelonDTO rDTO = MelonDTO.builder().singer(singer).singerCnt(singerCnt).build();

            rList.add(rDTO);

            rDTO = null;
            doc = null;
        }

        rs = null;
        col = null;
        pipeline = null;

        log.info(this.getClass().getName() + ".getSingerSongCnt End!");

        return rList;
    }

    @Override
    public List<MelonDTO> getSingerSong(String colNm, MelonDTO pDTO) throws Exception {

        log.info(this.getClass().getName() + ".getSingerSong Start!");

        // 조회 결과를 전달하기 위한 객체 생성하기
        List<MelonDTO> rList = new LinkedList<>();

        MongoCollection<Document> col = mongodb.getCollection(colNm);

        // 조회할 조건(SQL의 WHERE 역할 /  SELECT song, singer FROM MELON_20220321 where singer ='방탄소년단')
        Document query = new Document();
        query.append("singer", CmmUtil.nvl(pDTO.singer()));

        // 조회 결과 중 출력할 컬럼들(SQL의 SELECT절과 FROM절 가운데 컬럼들과 유사함)
        Document projection = new Document();
        projection.append("song", "$song");
        projection.append("singer", "$singer");

        // MongoDB는 무조건 ObjectId가 자동생성되며, ObjectID는 사용하지 않을때, 조회할 필요가 없음
        // ObjectId를 가지고 오지 않을 때 사용함
        projection.append("_id", 0);

        // MongoDB의 find 명령어를 통해 조회할 경우 사용함
        // 조회하는 데이터의 양이 적은 경우, find를 사용하고, 데이터양이 많은 경우 무조건 Aggregate 사용한다.
        FindIterable<Document> rs = col.find(query).projection(projection);

        for (Document doc : rs) {
            String song = CmmUtil.nvl(doc.getString("song"));
            String singer = CmmUtil.nvl(doc.getString("singer"));

            MelonDTO rDTO = MelonDTO.builder().song(song).singer(singer).build();

            // 레코드 결과를 List에 저장하기
            rList.add(rDTO);

        }
        log.info(this.getClass().getName() + ".getSingerSong End!");

        return rList;

    }

    @Override
    public int dropCollection(String colNm) throws Exception {

        log.info(this.getClass().getName() + ".dropCollection Start!");

        int res = 0;

        super.dropCollection(mongodb, colNm);

        res = 1;

        log.info(this.getClass().getName() + ".dropCollection End!");

        return res;
    }

    @Override
    public int insertManyField(String colNm, List<MelonDTO> pList) throws Exception {

        log.info(this.getClass().getName() + ".insertManyField Start!");

        int res = 0;

        // 데이터를 저장할 컬렉션 생성
        super.createCollection(mongodb, colNm, "collectTime");

        // 저장할 컬렉션 객체 생성
        MongoCollection<Document> col = mongodb.getCollection(colNm);

        List<Document> list = new ArrayList<>();

        // 람다식 활용하여 병렬 처리(순서 상관없이 저장) parallelStream과 -> 사용
        pList.parallelStream().forEach(melon ->
                list.add(new Document(new ObjectMapper().convertValue(melon, Map.class))));

        // ObjectMapper 이용한 List<MelonDTO> -> MeList<Document> 변경하기
//        List<Document> list = new ObjectMapper().convertValue(pList,
//                new TypeReference<>() {
//                });


        // List<Document> 파라미터로 사용하며, 레코드 리스트 단위로 한번에 저장하기
        col.insertMany(list);

        res = 1;

        log.info(this.getClass().getName() + ".insertManyField End!");

        return res;
    }

    @Override
    public int updateField(String colNm, MelonDTO pDTO) throws Exception {

        log.info(this.getClass().getName() + ".updateField Start!");

        int res = 0;

        MongoCollection<Document> col = mongodb.getCollection(colNm);

        String singer = CmmUtil.nvl(pDTO.singer());
        String updateSinger = CmmUtil.nvl(pDTO.updateSinger());

        log.info("pColNm : " + colNm);
        log.info("singer : " + singer);
        log.info("updateSinger : " + updateSinger);

        // 조회할 조건(SQL의 WHERE 역할 /  SELECT * FROM MELON_20220321 where singer ='방탄소년단')
        Document query = new Document();
        query.append("singer", singer);

        // MongoDB 데이터 수정은 반드시 컬렉션을 조회하고, 조회된 ObjectID를 기반으로 데이터를 수정함
        // MongoDB 환경은 분산환경(Sharding)으로 구성될 수 있기 때문에 정확한 PK에 매핑하기 위해서임
        FindIterable<Document> rs = col.find(query);

        // 람다식 활용하여 컬렉션에 조회된 데이터들을 수정하기
        rs.forEach(doc -> col.updateOne(doc, new Document("$set", new Document("singer", updateSinger))));

        res = 1;

        log.info(this.getClass().getName() + ".updateField End!");

        return res;
    }

    @Override
    public List<MelonDTO> getUpdateSinger(String colNm, MelonDTO pDTO) throws Exception {

        log.info(this.getClass().getName() + ".getUpdateSinger Start!");

        // 조회 결과를 전달하기 위한 객체 생성하기
        List<MelonDTO> rList = new LinkedList<>();

        MongoCollection<Document> col = mongodb.getCollection(colNm);

        // 조회할 조건(SQL의 WHERE 역할 /  SELECT song, singer FROM MELON_20220321 where singer ='방탄소년단')
        Document query = new Document();
        query.append("singer", CmmUtil.nvl(pDTO.updateSinger()));

        // 조회 결과 중 출력할 컬럼들(SQL의 SELECT절과 FROM절 가운데 컬럼들과 유사함)
        Document projection = new Document();
        projection.append("song", "$song");
        projection.append("singer", "$singer");

        // MongoDB는 무조건 ObjectId가 자동생성되며, ObjectID는 사용하지 않을때, 조회할 필요가 없음
        // ObjectId를 가지고 오지 않을 때 사용함
        projection.append("_id", 0);

        // MongoDB의 find 명령어를 통해 조회할 경우 사용함
        // 조회하는 데이터의 양이 적은 경우, find를 사용하고, 데이터양이 많은 경우 무조건 Aggregate 사용한다.
        FindIterable<Document> rs = col.find(query).projection(projection);

        for (Document doc : rs) {

            // MongoDB 조회 결과를 MelonDTO 저장하기 위해 변수에 저장
            String song = CmmUtil.nvl(doc.getString("song"));
            String singer = CmmUtil.nvl(doc.getString("singer"));

            log.info("song : " + song + "/ singer : " + singer);

            MelonDTO rDTO = MelonDTO.builder().song(song).singer(singer).build();

            // 레코드 결과를 List에 저장하기
            rList.add(rDTO);

        }
        log.info(this.getClass().getName() + ".getUpdateSinger End!");

        return rList;
    }

    @Override
    public int updateAddField(String colNm, MelonDTO pDTO) throws Exception {

        log.info(this.getClass().getName() + ".updateAddField Start!");

        int res = 0;

        MongoCollection<Document> col = mongodb.getCollection(colNm);

        String singer = CmmUtil.nvl(pDTO.singer());
        String nickname = CmmUtil.nvl(pDTO.nickname());

        log.info("pColNm : " + colNm);
        log.info("singer : " + singer);
        log.info("nickname : " + nickname);

        // 조회할 조건(SQL의 WHERE 역할 /  SELECT * FROM MELON_20220321 where singer ='방탄소년단')
        Document query = new Document();
        query.append("singer", singer);

        // MongoDB 데이터 삭제는 반드시 컬렉션을 조회하고, 조회된 ObjectID를 기반으로 데이터를 삭제함
        // MongoDB 환경은 분산환경(Sharding)으로 구성될 수 있기 때문에 정확한 PK에 매핑하기 위해서임
        FindIterable<Document> rs = col.find(query);

        // 람다식 활용하여 컬렉션에 조회된 데이터들을 수정하기
        // MongoDB Driver는 MongoDB의 "$set" 함수를 대신할 자바 함수를 구현함
        rs.forEach(doc -> col.updateOne(doc, set("nickname", nickname)));

        res = 1;

        log.info(this.getClass().getName() + ".updateAddField End!");

        return res;
    }

    @Override
    public List<MelonDTO> getSingerSongNickname(String colNm, MelonDTO pDTO) throws Exception {

        log.info(this.getClass().getName() + ".getSingerSongNickname Start!");

        // 조회 결과를 전달하기 위한 객체 생성하기
        List<MelonDTO> rList = new LinkedList<>();

        MongoCollection<Document> col = mongodb.getCollection(colNm);

        // 조회할 조건(SQL의 WHERE 역할 /  SELECT song, singer FROM MELON_20220321 where singer ='방탄소년단')
        Document query = new Document();
        query.append("singer", CmmUtil.nvl(pDTO.singer()));

        // 조회 결과 중 출력할 컬럼들(SQL의 SELECT절과 FROM절 가운데 컬럼들과 유사함)
        Document projection = new Document();
        projection.append("song", "$song");
        projection.append("singer", "$singer");
        projection.append("nickname", "$nickname");

        // MongoDB는 무조건 ObjectId가 자동생성되며, ObjectID는 사용하지 않을때, 조회할 필요가 없음
        // ObjectId를 가지고 오지 않을 때 사용함
        projection.append("_id", 0);

        // MongoDB의 find 명령어를 통해 조회할 경우 사용함
        // 조회하는 데이터의 양이 적은 경우, find를 사용하고, 데이터양이 많은 경우 무조건 Aggregate 사용한다.
        FindIterable<Document> rs = col.find(query).projection(projection);

        for (Document doc : rs) {

            // MongoDB 조회 결과를 MelonDTO 저장하기 위해 변수에 저장
            String song = CmmUtil.nvl(doc.getString("song"));
            String singer = CmmUtil.nvl(doc.getString("singer"));
            String nickname = CmmUtil.nvl(doc.getString("nickname"));

            log.info("song : " + song + "/ singer : " + singer + "/ nickname : " + nickname);

            MelonDTO rDTO = MelonDTO.builder().song(song).singer(singer).nickname(nickname).build();

            // 레코드 결과를 List에 저장하기
            rList.add(rDTO);

        }
        log.info(this.getClass().getName() + ".getSingerSongNickname End!");

        return rList;
    }

    @Override
    public int updateAddListField(String colNm, MelonDTO pDTO) throws Exception {

        log.info(this.getClass().getName() + ".updateAddListField Start!");

        int res = 0;

        MongoCollection<Document> col = mongodb.getCollection(colNm);

        String singer = CmmUtil.nvl(pDTO.singer());
        List<String> member = pDTO.member();

        log.info("pColNm : " + colNm);
        log.info("pDTO : " + pDTO);

        // 조회할 조건(SQL의 WHERE 역할 /  SELECT * FROM MELON_20220321 where singer ='방탄소년단')
        Document query = new Document();
        query.append("singer", singer);

        // MongoDB 데이터 삭제는 반드시 컬렉션을 조회하고, 조회된 ObjectID를 기반으로 데이터를 삭제함
        // MongoDB 환경은 분산환경(Sharding)으로 구성될 수 있기 때문에 정확한 PK에 매핑하기 위해서임
        FindIterable<Document> rs = col.find(query);

        // 람다식 활용하여 컬렉션에 조회된 데이터들을 수정하기
        // List 구조는 String 구조와 동일하게 set에 List 객체를 저장하면 된다.
        // MongoDB의 저장단위는 Document 객체는 자바의 Map을 상속받아 구현한 것이며, Map 특징인 값은 모든 객체가 저장 가능하다.
        rs.forEach(doc -> col.updateOne(doc, set("member", member)));

        res = 1;

        log.info(this.getClass().getName() + ".updateAddListField End!");

        return res;
    }

    @Override
    public List<MelonDTO> getSingerSongMember(String colNm, MelonDTO pDTO) throws Exception {

        log.info(this.getClass().getName() + ".getSingerSongMember Start!");

        // 조회 결과를 전달하기 위한 객체 생성하기
        List<MelonDTO> rList = new LinkedList<>();

        MongoCollection<Document> col = mongodb.getCollection(colNm);

        // 조회할 조건(SQL의 WHERE 역할 /  SELECT song, singer FROM MELON_20220321 where singer ='방탄소년단')
        Document query = new Document();
        query.append("singer", CmmUtil.nvl(pDTO.singer()));

        // 조회 결과 중 출력할 컬럼들(SQL의 SELECT절과 FROM절 가운데 컬럼들과 유사함)
        Document projection = new Document();
        projection.append("song", "$song");
        projection.append("singer", "$singer");
        projection.append("member", "$member");

        // MongoDB는 무조건 ObjectId가 자동생성되며, ObjectID는 사용하지 않을때, 조회할 필요가 없음
        // ObjectId를 가지고 오지 않을 때 사용함
        projection.append("_id", 0);

        // MongoDB의 find 명령어를 통해 조회할 경우 사용함
        // 조회하는 데이터의 양이 적은 경우, find를 사용하고, 데이터양이 많은 경우 무조건 Aggregate 사용한다.
        FindIterable<Document> rs = col.find(query).projection(projection);

        for (Document doc : rs) {

            // MongoDB 조회 결과를 MelonDTO 저장하기 위해 변수에 저장
            String song = CmmUtil.nvl(doc.getString("song"));
            String singer = CmmUtil.nvl(doc.getString("singer"));
            List<String> member = doc.getList("member", String.class, new ArrayList<>());

            log.info("song : " + song + "/ singer : " + singer + "/ member : " + member);

            MelonDTO rDTO = MelonDTO.builder().song(song).singer(singer).member(member).build();

            // 레코드 결과를 List에 저장하기
            rList.add(rDTO);

        }
        log.info(this.getClass().getName() + ".getSingerSongMember End!");

        return rList;

    }

    @Override
    public int updateFieldAndAddField(String colNm, MelonDTO pDTO) throws Exception {
        log.info(this.getClass().getName() + ".updateFieldAndAddField Start!");

        int res = 0;

        MongoCollection<Document> col = mongodb.getCollection(colNm);

        String singer = CmmUtil.nvl(pDTO.singer());
        String updateSinger = CmmUtil.nvl(pDTO.updateSinger());
        String addFieldValue = CmmUtil.nvl(pDTO.addFieldValue());

        log.info("pColNm : " + colNm);
        log.info("pDTO : " + pDTO);

        // 조회할 조건(SQL의 WHERE 역할 /  SELECT * FROM MELON_20220321 where singer ='방탄소년단')
        Document query = new Document();
        query.append("singer", singer);

        // MongoDB 데이터 삭제는 반드시 컬렉션을 조회하고, 조회된 ObjectID를 기반으로 데이터를 삭제함
        // MongoDB 환경은 분산환경(Sharding)으로 구성될 수 있기 때문에 정확한 PK에 매핑하기 위해서임
        FindIterable<Document> rs = col.find(query);

        // 한줄로 append해서 수정할 필드 추가해도 되지만, 가독성이 떨어져 줄마다 append 함
        Document updateDoc = new Document();
        updateDoc.append("singer", updateSinger); // 기존 필드 수정
        updateDoc.append("addData", addFieldValue); // 신규 필드 추가

        rs.forEach(doc -> col.updateOne(doc, new Document("$set", updateDoc)));

        res = 1;

        log.info(this.getClass().getName() + ".updateFieldAndAddField End!");

        return res;

    }

    @Override
    public List<MelonDTO> getSingerSongAddData(String colNm, MelonDTO pDTO) throws Exception {

        log.info(this.getClass().getName() + ".getSingerSongAddData Start!");

        // 조회 결과를 전달하기 위한 객체 생성하기
        List<MelonDTO> rList = new LinkedList<>();

        MongoCollection<Document> col = mongodb.getCollection(colNm);

        // 조회할 조건(SQL의 WHERE 역할 /  SELECT song, singer FROM MELON_20220321 where singer ='방탄소년단')
        Document query = new Document();
        query.append("singer", CmmUtil.nvl(pDTO.updateSinger())); // 이전 실행에서 가수이름이 변경되어 변경시킬 값으로 적용

        // 조회 결과 중 출력할 컬럼들(SQL의 SELECT절과 FROM절 가운데 컬럼들과 유사함)
        Document projection = new Document();
        projection.append("song", "$song");
        projection.append("singer", "$singer");
        projection.append("addData", "$addData");

        // MongoDB는 무조건 ObjectId가 자동생성되며, ObjectID는 사용하지 않을때, 조회할 필요가 없음
        // ObjectId를 가지고 오지 않을 때 사용함
        projection.append("_id", 0);

        // MongoDB의 find 명령어를 통해 조회할 경우 사용함
        // 조회하는 데이터의 양이 적은 경우, find를 사용하고, 데이터양이 많은 경우 무조건 Aggregate 사용한다.
        FindIterable<Document> rs = col.find(query).projection(projection);

        for (Document doc : rs) {

            // MongoDB 조회 결과를 MelonDTO 저장하기 위해 변수에 저장
            String song = CmmUtil.nvl(doc.getString("song"));
            String singer = CmmUtil.nvl(doc.getString("singer"));
            String addData = CmmUtil.nvl(doc.getString("addData"));

            log.info("song : " + song + "/ singer : " + singer + "/ addData : " + addData);

            MelonDTO rDTO = MelonDTO.builder().song(song).singer(singer).addFieldValue(addData).build();

            // 레코드 결과를 List에 저장하기
            rList.add(rDTO);

        }
        log.info(this.getClass().getName() + ".getSingerSongAddData End!");

        return rList;
    }

    @Override
    public int deleteDocument(String colNm, MelonDTO pDTO) throws Exception {

        log.info(this.getClass().getName() + ".deleteDocument Start!");

        int res = 0;

        MongoCollection<Document> col = mongodb.getCollection(colNm);

        String singer = CmmUtil.nvl(pDTO.singer());

        log.info("pColNm : " + colNm);
        log.info("pDTO : " + pDTO);

        // 조회할 조건(SQL의 WHERE 역할 /  SELECT * FROM MELON_20220321 where singer ='방탄소년단')
        Document query = new Document();
        query.append("singer", singer);

        // MongoDB 데이터 삭제는 반드시 컬렉션을 조회하고, 조회된 ObjectID를 기반으로 데이터를 삭제함
        // MongoDB 환경은 분산환경(Sharding)으로 구성될 수 있기 때문에 정확한 PK에 매핑하기 위해서임
        FindIterable<Document> rs = col.find(query);

        // 람다식 활용하여 데이터 삭제하기
        // 전체 컬렉션에 있는 데이터들을 삭제하기
        rs.forEach(col::deleteOne); // Col 객체에 자동으로 매칭되어 실행될 함수 정의
//        rs.forEach(doc -> col.deleteOne(doc)); // rs.forEach(col::deleteOne); 동일한 문법
        res = 1;

        log.info(this.getClass().getName() + ".deleteDocument End!");

        return res;
    }

}


