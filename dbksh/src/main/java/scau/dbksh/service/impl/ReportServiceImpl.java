package scau.dbksh.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import scau.dbksh.dto.AdminReportDTO;
import scau.dbksh.dto.ReportCreateDTO;
import scau.dbksh.dto.ReportHandleDTO;
import scau.dbksh.dto.UserDTO;
import scau.dbksh.entity.Product;
import scau.dbksh.entity.Report;
import scau.dbksh.mapper.ProductMapper;
import scau.dbksh.mapper.ReportMapper;
import scau.dbksh.result.Result;
import scau.dbksh.service.ReportService;
import scau.dbksh.utils.RoleUtils;
import scau.dbksh.utils.UserHolder;

import java.util.List;

@Service
public class ReportServiceImpl implements ReportService {

    private static final String REPORT_STATUS_PENDING = "\u5f85\u5904\u7406";
    private static final String REPORT_STATUS_PROCESSED = "\u5df2\u5904\u7406";
    private static final String PRODUCT_STATUS_REVIEWING = "\u5ba1\u6838\u4e2d";
    private static final String PRODUCT_STATUS_PUBLISHED = "\u5df2\u4e0a\u67b6";
    private static final String PRODUCT_STATUS_OFF_SHELF = "\u5df2\u4e0b\u67b6";
    private final ReportMapper reportMapper;
    private final ProductMapper productMapper;

    public ReportServiceImpl(ReportMapper reportMapper, ProductMapper productMapper) {
        this.reportMapper = reportMapper;
        this.productMapper = productMapper;
    }

    @Override
    @Transactional
    public Result<Long> createReport(ReportCreateDTO dto) {
        if (dto == null) {
            return Result.error("request body cannot be null");
        }
        if (dto.getProductId() == null || dto.getProductId() <= 0) {
            return Result.error("invalid product id");
        }

        String reason = trimToNull(dto.getReason());
        if (reason == null) {
            return Result.error("reason cannot be blank");
        }

        UserDTO user = UserHolder.getUser();
        if (user == null || user.getId() == null) {
            return Result.error("user not logged in");
        }

        Product product = productMapper.selectById(dto.getProductId());
        if (product == null) {
            return Result.error("product not found");
        }
        if (PRODUCT_STATUS_OFF_SHELF.equals(product.getStatus())) {
            return Result.error("product is off shelf");
        }
        if (!PRODUCT_STATUS_PUBLISHED.equals(product.getStatus())
                && !PRODUCT_STATUS_REVIEWING.equals(product.getStatus())) {
            return Result.error("product status cannot be reported");
        }

        int pendingCount = reportMapper.countPendingByUserIdAndProductId(
                user.getId(),
                dto.getProductId(),
                REPORT_STATUS_PENDING
        );
        if (pendingCount > 0) {
            return Result.error("duplicate pending report");
        }

        Report report = new Report();
        report.setUserId(user.getId());
        report.setProductId(product.getId());
        report.setSellerId(product.getSellerId());
        report.setReason(reason);
        report.setStatus(REPORT_STATUS_PENDING);

        int rows = reportMapper.insertReport(report);
        if (rows != 1 || report.getId() == null) {
            return Result.error("create report failed");
        }

        if (PRODUCT_STATUS_PUBLISHED.equals(product.getStatus())) {
            int updatedRows = productMapper.updateStatusById(product.getId(), PRODUCT_STATUS_REVIEWING);
            if (updatedRows != 1) {
                return Result.error("update product status failed");
            }
        }

        return Result.success(report.getId());
    }

    @Override
    public Result<List<AdminReportDTO>> listReportsForAdmin() {
        Result<Void> adminCheck = ensureAdminUser();
        if (adminCheck != null) {
            return Result.error(adminCheck.getMsg());
        }
        return Result.success(reportMapper.selectAllForAdmin());
    }

    @Override
    @Transactional
    public Result<Void> handleReport(Long id, ReportHandleDTO dto) {
        if (id == null || id <= 0) {
            return Result.error("invalid report id");
        }
        if (dto == null) {
            return Result.error("request body cannot be null");
        }

        Result<Void> adminCheck = ensureAdminUser();
        if (adminCheck != null) {
            return adminCheck;
        }

        String productStatus = trimToNull(dto.getProductStatus());
        if (!PRODUCT_STATUS_PUBLISHED.equals(productStatus) && !PRODUCT_STATUS_OFF_SHELF.equals(productStatus)) {
            return Result.error("invalid product status");
        }

        Report report = reportMapper.selectById(id);
        if (report == null) {
            return Result.error("report not found");
        }
        if (REPORT_STATUS_PROCESSED.equals(report.getStatus())) {
            return Result.error("report already processed");
        }

        Product product = productMapper.selectById(report.getProductId());
        if (product == null) {
            return Result.error("product not found");
        }

        if (!productStatus.equals(product.getStatus())) {
            int updatedRows = productMapper.updateStatusById(product.getId(), productStatus);
            if (updatedRows != 1) {
                return Result.error("update product status failed");
            }
        }

        int reportRows = reportMapper.updateStatusByProductId(
                report.getProductId(),
                REPORT_STATUS_PENDING,
                REPORT_STATUS_PROCESSED
        );
        if (reportRows < 1) {
            return Result.error("update report status failed");
        }

        return Result.success();
    }

    private Result<Void> ensureAdminUser() {
        UserDTO user = UserHolder.getUser();
        if (user == null || user.getId() == null) {
            return Result.error("user not logged in");
        }
        if (!RoleUtils.isAdminRole(user.getRole())) {
            return Result.error("forbidden");
        }
        return null;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
