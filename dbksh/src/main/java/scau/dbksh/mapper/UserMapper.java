package scau.dbksh.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import scau.dbksh.entity.User;

@Mapper
public interface UserMapper {

    User selectByWechat(@Param("wechat") String wechat);

    int insertUser(User user);
}
