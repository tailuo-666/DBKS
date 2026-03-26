package scau.dbksh.service;

import scau.dbksh.dto.LoginFormDTO;
import scau.dbksh.dto.UserDTO;
import scau.dbksh.result.Result;

public interface UserService {

    Result<Void> sendCode(String wechat);

    Result<String> userLogin(LoginFormDTO loginFormDTO);

    Result<String> adminLogin(LoginFormDTO loginFormDTO);

    Result<UserDTO> me();
}
