package org.poolpool.mohaeng;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// React Router 설정
// React Router가 페이지를 렌더링하기 위함
// React build가 static에 있어야 함
@Controller
public class WebController {

	// 루트 접속 및 React Router 경로 처리
    @GetMapping(value = { "/", "/{path:[^\\.]*}", "/**/{path:[^\\.]*}" })
    public String root() {
        return "forward:/index.html";
    }

}
