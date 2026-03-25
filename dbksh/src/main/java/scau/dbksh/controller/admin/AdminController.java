package scau.dbksh.controller.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import scau.dbksh.dto.AdminReportDTO;
import scau.dbksh.dto.ReportHandleDTO;
import scau.dbksh.dto.UserDTO;
import scau.dbksh.result.Result;
import scau.dbksh.service.ReportService;
import scau.dbksh.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final ReportService reportService;

    public AdminController(UserService userService, ReportService reportService) {
        this.userService = userService;
        this.reportService = reportService;
    }

    @GetMapping("/me")
    public Result<UserDTO> me() {
        return userService.me();
    }

    @GetMapping("/reports")
    public Result<List<AdminReportDTO>> listReports() {
        return reportService.listReportsForAdmin();
    }

    @PutMapping("/reports/{id}")
    public Result<Void> handleReport(@PathVariable("id") Long id, @RequestBody ReportHandleDTO dto) {
        return reportService.handleReport(id, dto);
    }
}
