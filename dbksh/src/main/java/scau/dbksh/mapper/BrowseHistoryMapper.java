package scau.dbksh.mapper;

import org.apache.ibatis.annotations.Mapper;
import scau.dbksh.entity.BrowseHistory;

@Mapper
public interface BrowseHistoryMapper {

    int insertBrowseHistory(BrowseHistory browseHistory);
}
