package scau.dbksh.service;

import scau.dbksh.dto.LoginFormDTO;
import scau.dbksh.dto.UserDTO;
import scau.dbksh.result.Result;

public interface UserService {

    Result<Void> sendCode(String wechat);

    Result<String> login(LoginFormDTO loginFormDTO);

    Result<UserDTO> me();
}
