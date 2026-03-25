package scau.dbksh.service;

import scau.dbksh.dto.AdminReportDTO;
import scau.dbksh.dto.ReportCreateDTO;
import scau.dbksh.dto.ReportHandleDTO;
import scau.dbksh.result.Result;

import java.util.List;

public interface ReportService {

    Result<Long> createReport(ReportCreateDTO dto);

    Result<List<AdminReportDTO>> listReportsForAdmin();

    Result<Void> handleReport(Long id, ReportHandleDTO dto);
}
