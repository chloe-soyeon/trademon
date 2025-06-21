package kr.trademon.service.impl;

import kr.trademon.dto.MailDTO;
import kr.trademon.dto.UserInfoDTO;
import kr.trademon.mapper.IUserInfoMapper;
import kr.trademon.service.IMailService;
import kr.trademon.service.IUserInfoService;
import kr.trademon.util.CmmUtil;
import kr.trademon.util.DateUtil;
import kr.trademon.util.EncryptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserInfoService implements IUserInfoService {
    private final IUserInfoMapper userInfoMapper; // 회원관련 SQL 사용하기 위한 Mapper 가져오기

    private final IMailService mailService; //메일 발송을 위한 MailService 자바 객체 가져오기

    @Override
    public UserInfoDTO getEmailExists(UserInfoDTO pDTO) throws Exception {

        log.info(this.getClass().getName() + ".getEmailExists Start!");

        // DB 이메일이 존재하는지 확인
        UserInfoDTO rDTO = userInfoMapper.getEmailExists(pDTO);

        // 결과값이 Null이면 기본값 세팅
        if (rDTO == null) {
            rDTO = new UserInfoDTO();
            rDTO.setExistsYn("N");
        }

        log.info("existsYn : " + rDTO.getExistsYn());
        log.info(this.getClass().getName() + ".getEmailExists End!");

        return rDTO;
    }

    @Override
    public UserInfoDTO sendEmailAuth(UserInfoDTO pDTO) throws Exception {

        log.info(this.getClass().getName() + ".sendEmailAuth Start!");

        // 중복 확인
        UserInfoDTO rDTO = userInfoMapper.getEmailExists(pDTO);
        String existsYn = CmmUtil.nvl(rDTO.getExistsYn());

        log.info("existsYn : " + existsYn);

        UserInfoDTO resDTO = new UserInfoDTO();
        resDTO.setExistsYn(existsYn);

        // 중복 아님 = 인증번호 발송 대상
        if ("N".equals(existsYn)) {
            int authNumber = ThreadLocalRandom.current().nextInt(100000, 1000000);
            resDTO.setAuthNumber(authNumber);

            MailDTO dto = new MailDTO();
            dto.setTitle("TradeMon 이메일 인증번호");

            // 🔥 여기부터 HTML 내용 작성
            String contents = "";
            contents += "<div style='max-width:600px; margin:0 auto; padding:40px 30px; font-family:Arial, sans-serif; border:1px solid #e0e0e0; border-radius:10px; box-shadow:0 2px 8px rgba(0,0,0,0.1);'>";
            contents += "    <div style='text-align:center;'>";
//            contents += "        <img src='/images/logos/main' alt='TradeMon' style='width:120px; margin-bottom:20px;'>";
            contents += "        <h2 style='color:#1A3365; margin-bottom:10px;'>TradeMon 이메일 주소 인증</h2>";
            contents += "        <p style='font-size:16px; color:#333; margin-bottom:30px;'>아래 인증번호를 입력하여 이메일 인증을 완료해주세요.</p>";
            contents += "        <div style='font-size:24px; font-weight:bold; margin:20px 0; color:#1A3365;'>" + authNumber + "</div>";
            contents += "        <p style='font-size:12px; color:#aaa; margin-top:40px;'>본 메일은 발신전용입니다. 문의사항은 홈페이지를 통해 접수해주세요.<br>© TradeMon Team</p>";
            contents += "    </div>";
            contents += "</div>";

            dto.setContents(contents); // ✅ 여기에 HTML 내용 세팅

//            dto.setContents("인증번호는 " + authNumber + " 입니다."); 이전버젼
            dto.setToMail(EncryptUtil.decAES128CBC(pDTO.getUserEmail()));

            mailService.doSendMail(dto);
        }

        log.info(this.getClass().getName() + ".sendEmailAuth End!");
        return resDTO;
    }




    @Override
    public int insertUserInfo(UserInfoDTO pDTO) throws Exception {

        log.info(this.getClass().getName() + ".insertUserInfo Start!");

        // 회원가입 성공 : 1, 아이디 중복으로인한 가입 취소 : 2, 기타 에러 발생 : 0
        int res = 0;


        // 회원가입
        int success = userInfoMapper.insertUserInfo(pDTO);

        // db에 데이터가 등록되었다면(회원가입 성공했다면....
        if (success > 0) {
            res = 1;

            /*
             * #######################################################
             *        				메일 발송 로직 추가 시작!!
             * #######################################################
             */

            MailDTO mDTO = new MailDTO();

            //회원정보화면에서 입력받은 이메일 변수(아직 암호화되어 넘어오기 때문에 복호화 수행함)
            mDTO.setToMail(EncryptUtil.decAES128CBC(kr.trademon.util.CmmUtil.nvl(pDTO.getUserEmail())));

            mDTO.setTitle("회원가입을 축하드립니다."); //제목

            // ✨ 깔끔한 HTML로 메일 내용 작성
            String contents = "";
            contents += "<div style='max-width:600px; margin:0 auto; padding:50px 30px; font-family:Arial, sans-serif; text-align:center;'>";
//            contents += "    <div style='font-size:48px; color:#4CAF50; margin-bottom:20px;'>✔️</div>";
            contents += "    <h1 style='color:#1A3365; font-size:24px; margin-bottom:10px;'>TradeMon 회원가입을 축하드립니다!</h1>";
            contents += "    <p style='font-size:16px; color:#333; margin-bottom:30px;'>";
            contents +=          CmmUtil.nvl(pDTO.getUserName()) + "님, TradeMon에 가입이 완료되었습니다.<br>";
            contents += "       이제 즐거운 투자 생활을 시작해보세요.";
            contents += "    </p>";
            contents += "    <div style='margin-top:30px;'>";
            contents += "        <a href='http://localhost:11000/user/login' style='background-color:#1A3365; color:#fff; text-decoration:none; padding:12px 24px; font-size:16px; border-radius:5px;'>로그인 하러 가기</a>";
            contents += "    </div>";
            contents += "    <p style='font-size:12px; color:#aaa; margin-top:40px;'>본 메일은 발신전용입니다. 문의사항은 홈페이지를 통해 접수해주세요.<br>© TradeMon Team</p>";
            contents += "</div>";

            mDTO.setContents(contents);

            //메일 내용에 가입자 이름넣어서 내용 발송
//            mDTO.setContents(kr.trademon.util.CmmUtil.nvl(pDTO.getUserName()) + "님의 회원가입을 진심으로 축하드립니다.");


            //회원 가입이 성공했기 때문에 메일을 발송함
            mailService.doSendMail(mDTO);

            /*
             * #######################################################
             *        				메일 발송 로직 추가 끝!!
             * #######################################################
             */

        } else {
            res = 0;

        }

        log.info(this.getClass().getName() + ".insertUserInfo End!");

        return res;
    }

    /**
     * 로그인을 위해 아이디와 비밀번호가 일치하는지 확인하기
     *
     * @param pDTO 로그인을 위한 회원아이디, 비밀번호
     * @return 로그인된 회원아이디 정보
     */
    @Override
    public UserInfoDTO getLogin(UserInfoDTO pDTO) throws Exception {

        log.info(this.getClass().getName() + ".getLogin Start!");

        // 로그인을 위해 아이디와 비밀번호가 일치하는지 확인하기 위한 mapper 호출하기
        // userInfoMapper.getUserLoginCheck(pDTO) 함수 실행 결과가 NUll 발생하면, UserInfoDTO 메모리에 올리기
        UserInfoDTO rDTO = Optional.ofNullable(userInfoMapper.getLogin(pDTO)).orElseGet(UserInfoDTO::new);

        /*
         * userInfoMapper로 부터 SELECT 쿼리의 결과로 회원아이디를 받아왔다면, 로그인 성공!!
         *
         * DTO의 변수에 값이 있는지 확인하기 처리속도 측면에서 가장 좋은 방법은 변수의 길이를 가져오는 것이다.
         * 따라서  .length() 함수를 통해 회원아이디의 글자수를 가져와 0보다 큰지 비교한다.
         * 0보다 크다면, 글자가 존재하는 것이기 때문에 값이 존재한다.
         */
        if (kr.trademon.util.CmmUtil.nvl(rDTO.getUserEmail()).length() > 0) {

            MailDTO mDTO = new MailDTO();

//            //아이디, 패스워드 일치하는지 체크하는 쿼리에서 이메일 값 받아오기(아직 암호화되어 넘어오기 때문에 복호화 수행함)
//            mDTO.setToMail(EncryptUtil.decAES128CBC(CmmUtil.nvl(rDTO.getUserEmail())));
//
//            mDTO.setTitle("로그인 알림!"); //제목
//
//            //메일 내용에 가입자 이름넣어서 내용 발송
//            mDTO.setContents(DateUtil.getDateTime("yyyy.MM.dd hh:mm:ss") + "에 "
//                    + CmmUtil.nvl(rDTO.getUserName()) + "님이 로그인하였습니다.");
//
//            //회원 가입이 성공했기 때문에 메일을 발송함
//            mailService.doSendMail(mDTO);

        }

        log.info(this.getClass().getName() + ".getLogin End!");

        return rDTO;
    }

    @Override
    public UserInfoDTO searchUserEmailOrPasswordProc(UserInfoDTO pDTO) throws Exception {

        log.info(this.getClass().getName() + ".searchUserEmailOrPasswordProc Start!");

        UserInfoDTO rDTO = userInfoMapper.getUserEmail(pDTO);

        log.info(this.getClass().getName() + ".searchUserEmailOrPasswordProc End!");

        return rDTO;
    }

    @Override
    public int newPasswordProc(UserInfoDTO pDTO) throws Exception {

        log.info(this.getClass().getName() + ".newPasswordProc Start!");

        // 비밀번호 재설정
        int success = userInfoMapper.updatePassword(pDTO);

        log.info(this.getClass().getName() + ".newPasswordProc End!");

        return success;
    }


}
