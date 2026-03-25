package scau.dbksh.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import scau.dbksh.dto.ReportCreateDTO;
import scau.dbksh.dto.ReportHandleDTO;
import scau.dbksh.dto.UserDTO;
import scau.dbksh.entity.Product;
import scau.dbksh.entity.Report;
import scau.dbksh.mapper.ProductMapper;
import scau.dbksh.mapper.ReportMapper;
import scau.dbksh.result.Result;
import scau.dbksh.utils.UserHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    private static final String ROLE_USER = "\u7528\u6237\u7aef";
    private static final String ROLE_ADMIN = "\u7ba1\u7406\u5458";
    private static final String PRODUCT_STATUS_REVIEWING = "\u5ba1\u6838\u4e2d";
    private static final String PRODUCT_STATUS_PUBLISHED = "\u5df2\u4e0a\u67b6";
    private static final String PRODUCT_STATUS_OFF_SHELF = "\u5df2\u4e0b\u67b6";
    private static final String REPORT_STATUS_PENDING = "\u5f85\u5904\u7406";
    private static final String REPORT_STATUS_PROCESSED = "\u5df2\u5904\u7406";

    @Mock
    private ReportMapper reportMapper;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ReportServiceImpl reportService;

    @AfterEach
    void tearDown() {
        UserHolder.removeUser();
    }

    @Test
    void shouldCreateReportAndSetProductToReviewing() {
        UserHolder.saveUser(currentUser(7L, ROLE_USER));

        Product product = product(5L, 9L, PRODUCT_STATUS_PUBLISHED);
        when(productMapper.selectById(5L)).thenReturn(product);
        when(reportMapper.countPendingByUserIdAndProductId(7L, 5L, REPORT_STATUS_PENDING)).thenReturn(0);
        doAnswer(invocation -> {
            Report report = invocation.getArgument(0);
            report.setId(33L);
            return 1;
        }).when(reportMapper).insertReport(any(Report.class));
        when(productMapper.updateStatusById(5L, PRODUCT_STATUS_REVIEWING)).thenReturn(1);

        ReportCreateDTO dto = new ReportCreateDTO();
        dto.setProductId(5L);
        dto.setReason(" spam item ");

        Result<Long> result = reportService.createReport(dto);

        assertEquals(1, result.getCode());
        assertEquals(33L, result.getData());

        ArgumentCaptor<Report> reportCaptor = ArgumentCaptor.forClass(Report.class);
        verify(reportMapper).insertReport(reportCaptor.capture());
        assertEquals(7L, reportCaptor.getValue().getUserId());
        assertEquals(5L, reportCaptor.getValue().getProductId());
        assertEquals(9L, reportCaptor.getValue().getSellerId());
        assertEquals("spam item", reportCaptor.getValue().getReason());
        assertEquals(REPORT_STATUS_PENDING, reportCaptor.getValue().getStatus());
        verify(productMapper).updateStatusById(5L, PRODUCT_STATUS_REVIEWING);
    }

    @Test
    void shouldCreateReportWithoutChangingReviewingProductStatus() {
        UserHolder.saveUser(currentUser(7L, ROLE_USER));

        Product product = product(5L, 9L, PRODUCT_STATUS_REVIEWING);
        when(productMapper.selectById(5L)).thenReturn(product);
        when(reportMapper.countPendingByUserIdAndProductId(7L, 5L, REPORT_STATUS_PENDING)).thenReturn(0);
        doAnswer(invocation -> {
            Report report = invocation.getArgument(0);
            report.setId(34L);
            return 1;
        }).when(reportMapper).insertReport(any(Report.class));

        ReportCreateDTO dto = new ReportCreateDTO();
        dto.setProductId(5L);
        dto.setReason("duplicate detail");

        Result<Long> result = reportService.createReport(dto);

        assertEquals(1, result.getCode());
        assertEquals(34L, result.getData());
        verify(productMapper, never()).updateStatusById(5L, PRODUCT_STATUS_REVIEWING);
    }

    @Test
    void shouldRejectCreateWhenProductDoesNotExist() {
        UserHolder.saveUser(currentUser(7L, ROLE_USER));
        when(productMapper.selectById(5L)).thenReturn(null);

        ReportCreateDTO dto = new ReportCreateDTO();
        dto.setProductId(5L);
        dto.setReason("reason");

        Result<Long> result = reportService.createReport(dto);

        assertEquals(0, result.getCode());
        assertEquals("product not found", result.getMsg());
        verify(reportMapper, never()).insertReport(any(Report.class));
    }

    @Test
    void shouldRejectCreateWhenProductIsOffShelf() {
        UserHolder.saveUser(currentUser(7L, ROLE_USER));
        when(productMapper.selectById(5L)).thenReturn(product(5L, 9L, PRODUCT_STATUS_OFF_SHELF));

        ReportCreateDTO dto = new ReportCreateDTO();
        dto.setProductId(5L);
        dto.setReason("reason");

        Result<Long> result = reportService.createReport(dto);

        assertEquals(0, result.getCode());
        assertEquals("product is off shelf", result.getMsg());
        verify(reportMapper, never()).insertReport(any(Report.class));
    }

    @Test
    void shouldRejectCreateWhenPendingReportAlreadyExists() {
        UserHolder.saveUser(currentUser(7L, ROLE_USER));

        when(productMapper.selectById(5L)).thenReturn(product(5L, 9L, PRODUCT_STATUS_PUBLISHED));
        when(reportMapper.countPendingByUserIdAndProductId(7L, 5L, REPORT_STATUS_PENDING)).thenReturn(1);

        ReportCreateDTO dto = new ReportCreateDTO();
        dto.setProductId(5L);
        dto.setReason("reason");

        Result<Long> result = reportService.createReport(dto);

        assertEquals(0, result.getCode());
        assertEquals("duplicate pending report", result.getMsg());
        verify(reportMapper, never()).insertReport(any(Report.class));
    }

    @Test
    void shouldHandleReportAndCloseAllPendingReportsForProduct() {
        UserHolder.saveUser(currentUser(1L, ROLE_ADMIN));

        Report report = new Report();
        report.setId(8L);
        report.setProductId(5L);
        report.setStatus(REPORT_STATUS_PENDING);
        when(reportMapper.selectById(8L)).thenReturn(report);
        when(productMapper.selectById(5L)).thenReturn(product(5L, 9L, PRODUCT_STATUS_REVIEWING));
        when(productMapper.updateStatusById(5L, PRODUCT_STATUS_OFF_SHELF)).thenReturn(1);
        when(reportMapper.updateStatusByProductId(5L, REPORT_STATUS_PENDING, REPORT_STATUS_PROCESSED)).thenReturn(2);

        ReportHandleDTO dto = new ReportHandleDTO();
        dto.setProductStatus(PRODUCT_STATUS_OFF_SHELF);

        Result<Void> result = reportService.handleReport(8L, dto);

        assertEquals(1, result.getCode());
        assertNull(result.getData());
        verify(productMapper).updateStatusById(5L, PRODUCT_STATUS_OFF_SHELF);
        verify(reportMapper).updateStatusByProductId(5L, REPORT_STATUS_PENDING, REPORT_STATUS_PROCESSED);
    }

    @Test
    void shouldRejectHandleWhenUserIsNotAdmin() {
        UserHolder.saveUser(currentUser(1L, ROLE_USER));

        ReportHandleDTO dto = new ReportHandleDTO();
        dto.setProductStatus(PRODUCT_STATUS_PUBLISHED);

        Result<Void> result = reportService.handleReport(8L, dto);

        assertEquals(0, result.getCode());
        assertEquals("forbidden", result.getMsg());
        verify(reportMapper, never()).selectById(8L);
    }

    @Test
    void shouldRejectHandleWhenReportIsAlreadyProcessed() {
        UserHolder.saveUser(currentUser(1L, ROLE_ADMIN));

        Report report = new Report();
        report.setId(8L);
        report.setProductId(5L);
        report.setStatus(REPORT_STATUS_PROCESSED);
        when(reportMapper.selectById(8L)).thenReturn(report);

        ReportHandleDTO dto = new ReportHandleDTO();
        dto.setProductStatus(PRODUCT_STATUS_PUBLISHED);

        Result<Void> result = reportService.handleReport(8L, dto);

        assertEquals(0, result.getCode());
        assertEquals("report already processed", result.getMsg());
        verify(productMapper, never()).updateStatusById(5L, PRODUCT_STATUS_PUBLISHED);
    }

    @Test
    void shouldRejectHandleWhenProductStatusIsInvalid() {
        UserHolder.saveUser(currentUser(1L, ROLE_ADMIN));

        ReportHandleDTO dto = new ReportHandleDTO();
        dto.setProductStatus(PRODUCT_STATUS_REVIEWING);

        Result<Void> result = reportService.handleReport(8L, dto);

        assertEquals(0, result.getCode());
        assertEquals("invalid product status", result.getMsg());
        verify(reportMapper, never()).selectById(8L);
    }

    private Product product(Long id, Long sellerId, String status) {
        Product product = new Product();
        product.setId(id);
        product.setSellerId(sellerId);
        product.setStatus(status);
        return product;
    }

    private UserDTO currentUser(Long id, String role) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(id);
        userDTO.setUsername("user_" + id);
        userDTO.setRole(role);
        return userDTO;
    }
}
