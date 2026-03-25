package scau.dbksh.controller.user;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import scau.dbksh.dto.ReportCreateDTO;
import scau.dbksh.result.Result;
import scau.dbksh.service.ReportService;

@RestController
@RequestMapping("/user/report")
public class UserReportController {

    private final ReportService reportService;

    public UserReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    public Result<Long> create(@RequestBody ReportCreateDTO dto) {
        return reportService.createReport(dto);
    }
}
