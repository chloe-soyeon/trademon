package kr.trademon.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import kr.trademon.dto.MsgDTO;
import kr.trademon.dto.UserInfoDTO;
import kr.trademon.service.IUserInfoService;
import kr.trademon.util.CmmUtil;
import kr.trademon.util.EncryptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Optional;

@Slf4j
@RequestMapping(value="/user")
@RequiredArgsConstructor
@Controller
public class UserInfoController {

    private final IUserInfoService userInfoService;

    /**
     * 회원가입 화면으로 이동
     */
    @GetMapping(value = "userRegForm")
    public String userRegForm() {
        log.info(this.getClass().getName() + ".userRegForm Start!");
        return "user/userRegForm";
    }


    /**
     * 회원 가입 전 이메일 중복체크하기(Ajax를 통해 입력한 아이디 정보 받음)
     * 유효한 이메일인 확인하기 위해 입력된 이메일에 인증번호 포함하여 메일 발송
     */
    @ResponseBody
    @PostMapping(value = "getEmailExists")
    public UserInfoDTO getEmailExists(HttpServletRequest request) throws Exception {

        log.info(this.getClass().getName() + ".getEmailExists Start!");

        String userEmail = CmmUtil.nvl(request.getParameter("userEmail")); // 회원아이디

        log.info("userEmail : " + userEmail);

        UserInfoDTO pDTO = new UserInfoDTO();
        pDTO.setUserEmail(EncryptUtil.encAES128CBC(userEmail));

        // 입력된 이메일이 중복된 이메일인지 조회
        UserInfoDTO rDTO = Optional.ofNullable(userInfoService.getEmailExists(pDTO)).orElseGet(UserInfoDTO::new);

        log.info(this.getClass().getName() + ".getEmailExists End!");

        return rDTO;
    }

    /**
     * 회원가입 로직 처리
     */
    @ResponseBody
    @PostMapping(value = "insertUserInfo")
    public MsgDTO insertUserInfo(HttpServletRequest request) throws Exception {

        log.info(this.getClass().getName() + ".insertUserInfo start!");

        int res = 0; // 회원가입 결과
        String msg = ""; //회원가입 결과에 대한 메시지를 전달할 변수
        MsgDTO dto = null; // 결과 메시지 구조

        //웹(회원정보 입력화면)에서 받는 정보를 저장할 변수
        UserInfoDTO pDTO = null;

        try {

            String userEmail = CmmUtil.nvl(request.getParameter("userEmail")); //이메일
            String userName = CmmUtil.nvl(request.getParameter("userName")); //이름
            String userPwd = CmmUtil.nvl(request.getParameter("userPwd")); //비밀번호
            String userPnum = CmmUtil.nvl(request.getParameter("userPnum")); //휴대폰 번호

            log.info("userEmail : " + userEmail);
            log.info("userName : " + userName);
            log.info("UserPwd : " + userPwd);
            log.info("userPnum : " + userPnum);


            //웹(회원정보 입력화면)에서 받는 정보를 저장할 변수를 메모리에 올리기
            pDTO = new UserInfoDTO();

            pDTO.setUserEmail(userEmail);
            pDTO.setUserName(userName);
            pDTO.setUserPwd(userPwd);
            pDTO.setUserPnum(userPnum);

            //비밀번호는 절대로 복호화되지 않도록 해시 알고리즘으로 암호화함
            pDTO.setUserPwd(EncryptUtil.encHashSHA256(userPwd));

            //민감 정보인 이메일은 AES128-CBC로 암호화함
            pDTO.setUserEmail(EncryptUtil.encAES128CBC(userEmail));


            /*
             * 회원가입
             * */
            res = userInfoService.insertUserInfo(pDTO);

            log.info("회원가입 결과(res) : " + res);

            if (res == 1) {
                msg = "반가워요! TradeMon의 회원이 되셨습니다😊";

                //추후 회원가입 입력화면에서 ajax를 활용해서 아이디 중복, 이메일 중복을 체크하길 바람
            } else if (res == 2) {
                msg = "이미 가입된 아이디입니다.";

            } else {
                msg = "오류로 인해 회원가입이 실패하였습니다.";

            }

        } catch (Exception e) {
            //저장이 실패되면 사용자에게 보여줄 메시지
            msg = "실패하였습니다. : " + e;
            log.info(e.toString());
            e.printStackTrace();

        } finally {
            // 결과 메시지 전달하기
            dto = new MsgDTO();
            dto.setResult(res);
            dto.setMsg(msg);

            log.info(this.getClass().getName() + ".insertUserInfo End!");
        }
        return dto;
    }

    /**
     * 이메일 인증번호 전송하기
     */
    @ResponseBody
    @PostMapping(value = "send-auth")
    public UserInfoDTO sendEmailAuth(HttpServletRequest request) throws Exception {

        log.info(this.getClass().getName() + ".sendEmailAuth Start!");

        String userEmail = CmmUtil.nvl(request.getParameter("userEmail"));

        log.info("userEmail : " + userEmail);

        UserInfoDTO pDTO = new UserInfoDTO();
        pDTO.setUserEmail(EncryptUtil.encAES128CBC(userEmail)); // 암호화해서 DTO에 저장

        UserInfoDTO rDTO = userInfoService.sendEmailAuth(pDTO); // 인증번호 발송 서비스 호출

        log.info(this.getClass().getName() + ".sendEmailAuth End!");

        return rDTO;
    }

    /**
     * 로그인을 위한 입력 화면으로 이동
     */
    @GetMapping(value = "login")
    public String login() {
        log.info(this.getClass().getName() + ".user/login Start!");

        log.info(this.getClass().getName() + ".user/login End!");

        return "user/login";
    }


    /**
     * 로그인 처리 및 결과 알려주는 화면으로 이동
     */
    @ResponseBody
    @PostMapping(value = "loginProc")
    public MsgDTO loginProc(HttpServletRequest request, HttpSession session) {

        log.info(this.getClass().getName() + ".loginProc Start!");

        int res = 0; //로그인 처리 결과를 저장할 변수 (로그인 성공 : 1, 아이디, 비밀번호 불일치로인한 실패 : 0, 시스템 에러 : 2)
        String msg = ""; //로그인 결과에 대한 메시지를 전달할 변수
        MsgDTO dto = null; // 결과 메시지 구조

        //웹(회원정보 입력화면)에서 받는 정보를 저장할 변수
        UserInfoDTO pDTO = null;

        try {

            String userEmail = CmmUtil.nvl(request.getParameter("userEmail")); //아이디
            String userPwd = CmmUtil.nvl(request.getParameter("userPwd")); //비밀번호

            log.info("userEmail : " + userEmail);
            log.info("userPwd : " + userPwd);

            //웹(회원정보 입력화면)에서 받는 정보를 저장할 변수를 메모리에 올리기
            pDTO = new UserInfoDTO();

            pDTO.setUserEmail(EncryptUtil.encAES128CBC(userEmail)); // 로그인 시에도 암호화

            //비밀번호는 절대로 복호화되지 않도록 해시 알고리즘으로 암호화함
            pDTO.setUserPwd(EncryptUtil.encHashSHA256(userPwd));

            // 로그인을 위해 아이디와 비밀번호가 일치하는지 확인하기 위한 userInfoService 호출하기
            UserInfoDTO rDTO = userInfoService.getLogin(pDTO);

            if (CmmUtil.nvl(rDTO.getUserEmail()).length() > 0) { //로그인 성공

                res = 1;

                // 사용자 이름 가져오기
                String userName = CmmUtil.nvl(rDTO.getUserName());

                // 세션에 이메일, 이름 저장
                session.setAttribute("SS_USER_EMAIL", userEmail);
                session.setAttribute("SS_USER_NAME", userName);

                // 사용자 맞춤 인사말 설정
                msg = userName + "님, 안녕하세요!😃";


            } else {
                msg = "아이디와 비밀번호가 올바르지 않습니다.";

            }

        } catch (Exception e) {
            //저장이 실패되면 사용자에게 보여줄 메시지
            msg = "시스템 문제로 로그인이 실패했습니다.";
            res = 2;
            log.info(e.toString());
            e.printStackTrace();

        } finally {
            // 결과 메시지 전달하기
            dto = new MsgDTO();
            dto.setResult(res);
            dto.setMsg(msg);

            log.info(this.getClass().getName() + ".loginProc End!");
        }

        return dto;
    }

    /**
     * 로그인 성공 페이지 이동
     */
    @GetMapping(value = "index")
    public String loginSuccess() {
        log.info(this.getClass().getName() + ".user/loginResult Start!");

        log.info(this.getClass().getName() + ".user/loginResult End!");

        return "user/index";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); // 세션 초기화 (로그아웃)
        return "redirect:/user/index"; // 메인 페이지로 리디렉션
    }

    /**
     * 이메일 찾기 화면
     */
    @GetMapping(value = "searchUserEmail")
    public String searchUserEmail() {
        log.info(this.getClass().getName() + ".user/searchUserEmail Start!");

        log.info(this.getClass().getName() + ".user/searchUserEmail End!");

        return "user/searchUserEmail";

    }

    @PostMapping(value = "searchUserEmailProc")
    public String searchUserEmailProc(HttpServletRequest request, ModelMap model) throws Exception {
        log.info(this.getClass().getName() + ".user/searchUserEmailProc Start!");

        String userName = CmmUtil.nvl(request.getParameter("userName"));
        String userPnum = CmmUtil.nvl(request.getParameter("userPnum"));

        log.info("userName : " + userName);
        log.info("userPnum : " + userPnum);

        UserInfoDTO pDTO = new UserInfoDTO();
        pDTO.setUserName(userName);
        pDTO.setUserPnum(userPnum);

        UserInfoDTO rDTO = Optional.ofNullable(userInfoService.searchUserEmailOrPasswordProc(pDTO))
                .orElseGet(UserInfoDTO::new);

        // ✅ 복호화 처리 추가
        if (rDTO.getUserEmail() != null && !rDTO.getUserEmail().isEmpty()) {
            String decryptedEmail = EncryptUtil.decAES128CBC(rDTO.getUserEmail());
            rDTO.setUserEmail(decryptedEmail); // 복호화된 이메일로 덮어씀
        }

        model.addAttribute("rDTO", rDTO);

        log.info(this.getClass().getName() + ".user/searchUserEmailProc End!");

        return "user/searchUserEmailResult";
    }

    /**
     * 비밀번호 찾기 화면
     */
    @GetMapping(value = "searchPassword")
    public String searchPassword(HttpSession session) {
        log.info(this.getClass().getName() + ".user/searchPassword Start!");

        // 강제 URL 입력 등 오는 경우가 있어 세션 삭제
        // 비밀번호 재생성하는 화면은 보안을 위해 생성한 NEW_PASSWORD 세션 삭제
        session.setAttribute("NEW_PASSWORD", "");
        session.removeAttribute("NEW_PASSWORD");

        log.info(this.getClass().getName() + ".user/searchPassword End!");

        return "user/searchPassword";

    }

    /**
     * 비밀번호 찾기 로직 수행
     * <p>
     * 아이디, 이름, 이메일 일치하면, 비밀번호 재발급 화면 이동
     */
    @PostMapping(value = "searchPasswordProc")
    public String searchPasswordProc(HttpServletRequest request, ModelMap model, HttpSession session) throws Exception {
        log.info(this.getClass().getName() + ".user/searchPasswordProc Start!");

        String userEmail = CmmUtil.nvl(request.getParameter("userEmail"));
        String userName = CmmUtil.nvl(request.getParameter("userName"));

        log.info("userEmail : " + userEmail);
        log.info("userName : " + userName);

        UserInfoDTO pDTO = new UserInfoDTO();
        pDTO.setUserName(userName);
        pDTO.setUserEmail(EncryptUtil.encAES128CBC(userEmail));

        UserInfoDTO rDTO = userInfoService.searchUserEmailOrPasswordProc(pDTO);

        if (rDTO == null || rDTO.getUserEmail() == null) {
            model.addAttribute("msg", "입력하신 이름과 이메일이 일치하지 않습니다.");
            model.addAttribute("error", true); // 에러 여부 표시
            return "user/searchPassword"; // 다시 현재 화면으로
        }

        model.addAttribute("rDTO", rDTO);
        session.setAttribute("NEW_PASSWORD", userEmail);

        log.info(this.getClass().getName() + ".user/searchPasswordProc End!");

        return "user/newPassword";
    }



    /**
     * 비밀번호 찾기 로직 수행
     * <p>
     * 아이디, 이름, 이메일 일치하면, 비밀번호 재발급 화면 이동
     */
    @PostMapping(value = "newPasswordProc")
    public String newPasswordProc(HttpServletRequest request, ModelMap model, HttpSession session) throws Exception {

        log.info(this.getClass().getName() + ".user/newPasswordProc Start!");

        String msg = ""; // 웹에 보여줄 메시지

        // 정상적인 접근인지 체크
        String newPassword = CmmUtil.nvl((String) session.getAttribute("NEW_PASSWORD"));

        if (newPassword.length() > 0) { //정상 접근


            String password = CmmUtil.nvl(request.getParameter("password")); // 신규 비밀번호


            log.info("password : " + password);



            UserInfoDTO pDTO = new UserInfoDTO();
            pDTO.setUserEmail(EncryptUtil.encAES128CBC(newPassword));
//            pDTO.setUserEmail(newPassword);
            pDTO.setUserPwd(EncryptUtil.encHashSHA256(password));

            userInfoService.newPasswordProc(pDTO);

            // 비밀번호 재생성하는 화면은 보안을 위해 생성한 NEW_PASSWORD 세션 삭제
            session.setAttribute("NEW_PASSWORD", "");
            session.removeAttribute("NEW_PASSWORD");

            msg = "비밀번호가 재설정되었습니다.";

        } else { // 비정상 접근
            msg = "비정상 접근입니다.";
        }

        model.addAttribute("msg", msg);


        log.info(this.getClass().getName() + ".user/newPasswordProc End!");

        return "user/newPasswordResult";

    }





}
