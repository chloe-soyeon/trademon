package kr.trademon.service;

import kr.trademon.dto.MailDTO;

public interface IMailService {
    //메일 발송
    int doSendMail(MailDTO pDTO);
}
