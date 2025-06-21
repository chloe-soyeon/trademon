package kr.trademon.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class UserInfoDTO {
    private String userEmail;
    private String userPwd;
    private String userName;
    private String userPnum;
    private LocalDateTime regDt;

    //회원 가입 시, 중복 가입을 방지 위해 사용할 변수
    private String existsYn;

    // 이메일 중복 체크를 위한 인증번호
    private int authNumber;
}
