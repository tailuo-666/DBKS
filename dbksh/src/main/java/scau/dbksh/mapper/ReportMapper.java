package scau.dbksh.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import scau.dbksh.dto.AdminReportDTO;
import scau.dbksh.entity.Report;

import java.util.List;

@Mapper
public interface ReportMapper {

    int insertReport(Report report);

    Report selectById(@Param("id") Long id);

    int countPendingByUserIdAndProductId(
            @Param("userId") Long userId,
            @Param("productId") Long productId,
            @Param("status") String status
    );

    int updateStatusByProductId(
            @Param("productId") Long productId,
            @Param("fromStatus") String fromStatus,
            @Param("toStatus") String toStatus
    );

    List<AdminReportDTO> selectAllForAdmin();
}
