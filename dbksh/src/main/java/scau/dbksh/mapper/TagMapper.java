package scau.dbksh.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import scau.dbksh.entity.Tag;

import java.util.List;

@Mapper
public interface TagMapper {

    List<Tag> selectByNames(@Param("names") List<String> names);

    List<Tag> selectByNameLike(@Param("keyword") String keyword);

    Tag selectByName(@Param("name") String name);

    int insertTag(Tag tag);
}
