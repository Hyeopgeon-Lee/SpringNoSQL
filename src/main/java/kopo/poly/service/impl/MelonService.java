package kopo.poly.service.impl;

import kopo.poly.dto.MelonDTO;
import kopo.poly.persistance.mongodb.IMelonMapper;
import kopo.poly.persistance.redis.IMelonCacheMapper;
import kopo.poly.service.IMelonService;
import kopo.poly.util.CmmUtil;
import kopo.poly.util.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class MelonService implements IMelonService {

    private final IMelonMapper melonMapper; // MongoDB에 저장할 Mapper

    private final IMelonCacheMapper melonCacheMapper; // RedisDB Mapper

    /**
     * 멜론 차트 수집 함수(웹 크롤링)
     */
    private List<MelonDTO> doCollect() throws Exception {

        log.info("{}.doCollect Start!", this.getClass().getName());

        List<MelonDTO> pList = new LinkedList<>();

        // 멜론 Top100 중 50위까지 정보 가져오는 페이지
        String url = "https://www.melon.com/chart/index.htm";

        // JSOUP 라이브러리를 통해 사이트 접속되면, 그 사이트의 전체 HTML소스 저장할 변수
        Document doc = Jsoup.connect(url).get();

        // <div class="service_list_song"> 이 태그 내에서 있는 HTML소스만 element에 저장됨
        Elements element = doc.select("div.service_list_song");

        // Iterator을 사용하여 멜론차트 정보를 가져오기
        for (Element songInfo : element.select("div.wrap_song_info")) {

            // 크롤링을 통해 데이터 저장하기
            String song = CmmUtil.nvl(songInfo.select("div.ellipsis.rank01 a").text()); // 노래
            String singer = CmmUtil.nvl(songInfo.select("div.ellipsis.rank02 a").eq(0).text()); // 가수

            log.info("song : {}", song);
            log.info("singer : {}", singer);

            // 가수와 노래 정보가 모두 수집되었다면, 저장함
            if ((!song.isEmpty()) && (!singer.isEmpty())) {

                MelonDTO pDTO = MelonDTO.builder().collectTime(DateUtil.getDateTime("yyyyMMddhhmmss"))
                        .song(song).singer(singer).build();

                // 한번에 여러개의 데이터를 MongoDB에 저장할 List 형태의 데이터 저장하기
                pList.add(pDTO);

            }
        }

        log.info("{}.doCollect End!", this.getClass().getName());

        return pList;

    }

    /**
     * CacheEvict - 캐시 이름: "melonSongs"
     * - 키 생성기: "melonKeyGen"
     * - 동작:
     * - 해당 키의 캐시 엔트리를 삭제(무효화)하여, 다음 조회(@Cacheable)가 DB→캐시로 재채움되도록 함.
     * - 기본적으로 메서드가 정상 완료된 “후(after)”에 삭제 수행.
     * 예외가 나도 무조건 지우고 싶다면 beforeInvocation = true 옵션 사용.
     * - 사용 시점:
     * - 수집/저장/수정/삭제 등으로 DB의 오늘자 데이터가 바뀌는 메서드에 적용.
     * - 예: collectMelonSong() 끝에서 오늘 키를 비워 최신 데이터로 다시 채우게 함.
     */
    @CacheEvict(cacheNames = "melonSongs", keyGenerator = "melonKeyGen")
    @Override
    public int collectMelonSong() throws Exception {

        int res;

        // 생성할 컬렉션명
        String colNm = "MELON_" + DateUtil.getDateTime("yyyyMMdd");

        // private 함수로 선언된 doCollect 함수를 호출하여 결과를 받기
        List<MelonDTO> rList = this.doCollect();

        // MongoDB에 데이터저장하기
        res = melonMapper.insertSong(rList, colNm);

        // 로그 찍기(추후 찍은 로그를 통해 이 함수에 접근했는지 파악하기 용이하다.)
        log.info("{}.collectMelonSong End!", this.getClass().getName());

        return res;
    }

    /**
     * Cacheable
     * - 캐시 이름: "melonSongs"
     * - 키 생성기: "melonKeyGen" (예: "MELON_yyyyMMdd" 형식으로 생성)
     * - 동작:
     * 1) 같은 키가 캐시에 있으면 메서드를 호출하지 않고 캐시 값을 즉시 반환(HIT).
     * 2) 없으면( MISS ) 메서드를 "한 번만" 실행해 결과를 캐시에 저장한 뒤 반환.
     * sync = true 이면 동일 JVM 내에서 같은 키에 대한 동시 호출을 1회로 직렬화(캐시 스탬피드 방지).
     * 다중 서버/파드 환경에서는 인스턴스별로 한 번씩 실행될 수 있음(분산 락 아님).
     */
    @Cacheable(cacheNames = "melonSongs", keyGenerator = "melonKeyGen", sync = true)
    @Override
    public List<MelonDTO> getSongList() throws Exception {

        log.info("{}.getSongList Start!", this.getClass().getName());

        // MongoDB에 저장된 컬렉션 이름
        String colNm = "MELON_" + DateUtil.getDateTime("yyyyMMdd");

        List<MelonDTO> rList = melonMapper.getSongList(colNm); // MongoDB에서 데이터 가져오기

        log.info("{}.getSongList End!", this.getClass().getName());

        return rList;
    }

    @Override
    public List<MelonDTO> getSingerSongCnt() throws Exception {

        log.info("{}.getSingerSongCnt Start!", this.getClass().getName());

        String colNm = "MELON_" + DateUtil.getDateTime("yyyyMMdd");

        List<MelonDTO> rList = melonMapper.getSingerSongCnt(colNm);

        log.info("{}.getSingerSongCnt End!", this.getClass().getName());

        return rList;
    }

    @Override
    public int dropCollection() throws Exception {

        log.info("{}.dropCollection Start!", this.getClass().getName());

        int res;

        // MongoDB에 저장된 컬렉션 이름
        String colNm = "MELON_" + DateUtil.getDateTime("yyyyMMdd");

        // 기존 수집된 멜론Top100 수집한 컬렉션 삭제하기
        res = melonMapper.dropCollection(colNm);

        log.info("{}.dropCollection End!", this.getClass().getName());

        return res;
    }

    @Override
    public List<MelonDTO> getSingerSong(MelonDTO pDTO) throws Exception {

        log.info("{}.getSingerSong Start!", this.getClass().getName());

        // MongoDB에 저장된 컬렉션 이름
        String colNm = "MELON_" + DateUtil.getDateTime("yyyyMMdd");

        // 결과값
        List<MelonDTO> rList = null;

        // Melen 노래 수집하기
        if (this.collectMelonSong() == 1) {

            // 가수 노래 조회하기
            rList = melonMapper.getSingerSong(colNm, pDTO);

        }

        log.info("{}.getSingerSong End!", this.getClass().getName());

        return rList;
    }


    @Override
    public List<MelonDTO> insertManyField() throws Exception {

        // 로그 찍기(추후 찍은 로그를 통해 이 함수에 접근했는지 파악하기 용이하다.)
        log.info("{}.insertManyField Start!", this.getClass().getName());

        List<MelonDTO> rList = null; // 변경된 데이터 조회 결과

        // 생성할 컬렉션명
        String colNm = "MELON_" + DateUtil.getDateTime("yyyyMMdd");

        // MongoDB에 데이터저장하기
        if (melonMapper.insertManyField(colNm, this.doCollect()) == 1) {

            // 변경된 값을 확인하기 위해 MongoDB로부터 데이터 조회하기
            rList = melonMapper.getSongList(colNm);

        }

        // 로그 찍기(추후 찍은 로그를 통해 이 함수에 접근했는지 파악하기 용이하다.)
        log.info("{}.insertManyField End!", this.getClass().getName());

        return rList;
    }

    @Override
    public List<MelonDTO> updateField(MelonDTO pDTO) throws Exception {

        // 로그 찍기(추후 찍은 로그를 통해 이 함수에 접근했는지 파악하기 용이하다.)
        log.info("{}.updateField Start!", this.getClass().getName());

        List<MelonDTO> rList = null; // 변경된 데이터 조회 결과

        // 수정할 컬렉션
        String colNm = "MELON_" + DateUtil.getDateTime("yyyyMMdd");

        // 기존 수집된 멜론Top100 수집한 컬렉션 삭제하기
        melonMapper.dropCollection(colNm);

        // 멜론Top100 수집하기
        if (this.collectMelonSong() == 1) {

            // 예 : singer 필드에 저장된 '방탄소년단' 값을 'BTS'로 변경하기
            if (melonMapper.updateField(colNm, pDTO) == 1) {

                // 변경된 값을 확인하기 위해 MongoDB로부터 데이터 조회하기
                rList = melonMapper.getUpdateSinger(colNm, pDTO);

            }
        }

        // 로그 찍기(추후 찍은 로그를 통해 이 함수에 접근했는지 파악하기 용이하다.)
        log.info("{}.updateField End!", this.getClass().getName());

        return rList;
    }


    @Override
    public List<MelonDTO> updateAddField(MelonDTO pDTO) throws Exception {

        // 로그 찍기(추후 찍은 로그를 통해 이 함수에 접근했는지 파악하기 용이하다.)
        log.info("{}.updateAddField Start!", this.getClass().getName());

        List<MelonDTO> rList = null; // 변경된 데이터 조회 결과

        // 수정할 컬렉션
        String colNm = "MELON_" + DateUtil.getDateTime("yyyyMMdd");

        // 기존 수집된 멜론Top100 수집한 컬렉션 삭제하기
        melonMapper.dropCollection(colNm);

        // 멜론Top100 수집하기
        if (this.collectMelonSong() == 1) {

            // 예 : nickname 필드를 추가하고, nickname 필드 값은 'BTS' 저장하기
            if (melonMapper.updateAddField(colNm, pDTO) == 1) {

                // 변경된 값을 확인하기 위해 MongoDB로부터 데이터 조회하기
                rList = melonMapper.getSingerSongNickname(colNm, pDTO);

            }
        }

        // 로그 찍기(추후 찍은 로그를 통해 이 함수에 접근했는지 파악하기 용이하다.)
        log.info("{}.updateAddField End!", this.getClass().getName());

        return rList;
    }

    @Override
    public List<MelonDTO> updateAddListField(MelonDTO pDTO) throws Exception {

        // 로그 찍기(추후 찍은 로그를 통해 이 함수에 접근했는지 파악하기 용이하다.)
        log.info("{}.updateAddListField Start!", this.getClass().getName());

        List<MelonDTO> rList = null; // 변경된 데이터 조회 결과

        // 수정할 컬렉션
        String colNm = "MELON_" + DateUtil.getDateTime("yyyyMMdd");

        // 기존 수집된 멜론Top100 수집한 컬렉션 삭제하기
        melonMapper.dropCollection(colNm);

        // 멜론Top100 수집하기
        if (this.collectMelonSong() == 1) {

            // MongoDB에 데이터저장하기
            if (melonMapper.updateAddListField(colNm, pDTO) == 1) {

                // 변경된 값을 확인하기 위해 MongoDB로부터 데이터 조회하기
                rList = melonMapper.getSingerSongMember(colNm, pDTO);

            }
        }

        // 로그 찍기(추후 찍은 로그를 통해 이 함수에 접근했는지 파악하기 용이하다.)
        log.info("{}.updateAddListField End!", this.getClass().getName());

        return rList;
    }

    @Override
    public List<MelonDTO> updateFieldAndAddField(MelonDTO pDTO) throws Exception {
        // 로그 찍기(추후 찍은 로그를 통해 이 함수에 접근했는지 파악하기 용이하다.)
        log.info("{}.updateFieldAndAddField Start!", this.getClass().getName());

        List<MelonDTO> rList = null; // 변경된 데이터 조회 결과

        // 수정할 컬렉션
        String colNm = "MELON_" + DateUtil.getDateTime("yyyyMMdd");

        // 기존 수집된 멜론Top100 수집한 컬렉션 삭제하기
        melonMapper.dropCollection(colNm);

        // 멜론Top100 수집하기
        if (this.collectMelonSong() == 1) {

            // MongoDB에 데이터 수정하기
            if (melonMapper.updateFieldAndAddField(colNm, pDTO) == 1) {

                // 변경된 값을 확인하기 위해 MongoDB로부터 데이터 조회하기
                rList = melonMapper.getSingerSongAddData(colNm, pDTO);

            }
        }

        // 로그 찍기(추후 찍은 로그를 통해 이 함수에 접근했는지 파악하기 용이하다.)
        log.info("{}.updateFieldAndAddField End!", this.getClass().getName());

        return rList;
    }

    @Override
    public List<MelonDTO> deleteDocument(MelonDTO pDTO) throws Exception {

        // 로그 찍기(추후 찍은 로그를 통해 이 함수에 접근했는지 파악하기 용이하다.)
        log.info("{}.deleteDocument Start!", this.getClass().getName());

        List<MelonDTO> rList = null; // 변경된 데이터 조회 결과

        // 삭제할 컬렉션
        String colNm = "MELON_" + DateUtil.getDateTime("yyyyMMdd");

        // 기존 수집된 멜론Top100 수집한 컬렉션 삭제하기
        melonMapper.dropCollection(colNm);

        // 멜론Top100 수집하기
        if (this.collectMelonSong() == 1) {

            // MongoDB에 데이터 삭제하기
            if (melonMapper.deleteDocument(colNm, pDTO) == 1) {

                // 삭제된 값을 확인하기 위해 MongoDB로부터 데이터 조회하기
                rList = melonMapper.getSongList(colNm);

            }

        }
        // 로그 찍기(추후 찍은 로그를 통해 이 함수에 접근했는지 파악하기 용이하다.)
        log.info("{}.deleteDocument End!", this.getClass().getName());

        return rList;
    }
}
