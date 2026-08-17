package com.mes.factory.controller;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.mes.factory.model.*;
import com.mes.factory.repository.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    @Autowired
    private MachineRepository machineRepository;

    @Autowired
    private ProductionOrderRepository orderRepository;

    @Autowired
    private ProductionTrackingRepository trackingRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private RawMaterialRepository materialRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ProductRepository productRepository;

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardData() {
        Map<String, Object> data = new HashMap<>();

        // 1. Machines Running
        long runningMachines = machineRepository.findAll().stream()
                .filter(m -> "Running".equalsIgnoreCase(m.getStatus()))
                .count();

        // 2. Orders Pending
        long pendingOrders = orderRepository.findAll().stream()
                .filter(o -> "Pending".equalsIgnoreCase(o.getStatus()))
                .count();

        // 3. Today's Production (Completed quantity) & Defective Products
        List<ProductionTracking> trackings = trackingRepository.findAll();
        int totalCompleted = trackings.stream().mapToInt(ProductionTracking::getCompletedQuantity).sum();
        int totalDefective = trackings.stream().mapToInt(ProductionTracking::getDefectiveQuantity).sum();

        // 4. Active Workers (Total Employees for dashboard)
        long totalEmployees = employeeRepository.count();

        // 5. Inventory Status
        List<RawMaterial> materials = materialRepository.findAll();

        // 6. Recent Audit Logs
        List<AuditLog> auditLogs = auditLogRepository.findFirst50ByOrderByTimestampDesc();

        // 7. Machines list (for live display)
        List<Machine> machines = machineRepository.findAll();

        // 8. Orders list (for order management)
        List<ProductionOrder> orders = orderRepository.findAll();

        data.put("runningMachines", runningMachines);
        data.put("pendingOrders", pendingOrders);
        data.put("todayProduction", totalCompleted);
        data.put("defectiveProducts", totalDefective);
        data.put("activeWorkers", totalEmployees);
        data.put("inventory", materials);
        data.put("auditLogs", auditLogs);
        data.put("machines", machines);
        data.put("orders", orders);

        // Products list
        data.put("products", productRepository.findAll());

        // Worker Performance calculation
        List<Map<String, Object>> workerPerformance = new ArrayList<>();
        List<Employee> operators = employeeRepository.findByRole("OPERATOR");
        for (Employee op : operators) {
            List<ProductionTracking> opTrackings = trackingRepository.findAll().stream()
                    .filter(t -> op.getId().equals(t.getEmployeeId()))
                    .toList();
            int completed = opTrackings.stream().mapToInt(ProductionTracking::getCompletedQuantity).sum();
            int defective = opTrackings.stream().mapToInt(ProductionTracking::getDefectiveQuantity).sum();
            double eff = (completed + defective) > 0 ? ((double) completed / (completed + defective)) * 100 : 100.0;

            Map<String, Object> perf = new HashMap<>();
            perf.put("employeeId", op.getId());
            perf.put("name", op.getName());
            perf.put("completed", completed);
            perf.put("defective", defective);
            perf.put("efficiency", Math.round(eff * 10.0) / 10.0);
            workerPerformance.add(perf);
        }
        data.put("workerPerformance", workerPerformance);

        // Defect analysis map
        Map<String, Integer> defectSums = new HashMap<>();
        for (ProductionTracking t : trackings) {
            if (t.getDefectReasons() != null) {
                t.getDefectReasons().forEach((reason, val) -> {
                    defectSums.put(reason, defectSums.getOrDefault(reason, 0) + val);
                });
            }
        }
        data.put("defects", defectSums);

        return ResponseEntity.ok(data);
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Production Report");

            // Header Row
            Row headerRow = sheet.createRow(0);
            String[] headers = { "Order ID", "Product ID", "Target Quantity", "Completed", "Defective", "Status",
                    "Machine", "Operator" };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // Data Rows
            List<ProductionOrder> orders = orderRepository.findAll();
            int rowIdx = 1;
            for (ProductionOrder o : orders) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(o.getId());
                row.createCell(1).setCellValue(o.getProductId());
                row.createCell(2).setCellValue(o.getQuantity());

                // Find tracking
                Optional<ProductionTracking> trackOpt = trackingRepository.findByOrderId(o.getId());
                if (trackOpt.isPresent()) {
                    row.createCell(3).setCellValue(trackOpt.get().getCompletedQuantity());
                    row.createCell(4).setCellValue(trackOpt.get().getDefectiveQuantity());
                } else {
                    row.createCell(3).setCellValue(0);
                    row.createCell(4).setCellValue(0);
                }

                row.createCell(5).setCellValue(o.getStatus());
                row.createCell(6).setCellValue(o.getMachineId() != null ? o.getMachineId() : "N/A");
                row.createCell(7).setCellValue(o.getOperatorId() != null ? o.getOperatorId() : "N/A");
            }

            workbook.write(out);
            byte[] bytes = out.toByteArray();

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            responseHeaders.setContentDispositionFormData("attachment", "MES_Production_Report.xlsx");

            return ResponseEntity.ok()
                    .headers(responseHeaders)
                    .body(bytes);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            // Document Header
            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
            Paragraph title = new Paragraph("MES Production & Efficiency Report", titleFont);
            title.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("Generated: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())));
            document.add(new Paragraph(" "));

            // Statistics Table
            List<ProductionOrder> orders = orderRepository.findAll();
            List<ProductionTracking> trackings = trackingRepository.findAll();
            int totalComp = trackings.stream().mapToInt(ProductionTracking::getCompletedQuantity).sum();
            int totalDef = trackings.stream().mapToInt(ProductionTracking::getDefectiveQuantity).sum();
            double efficiency = totalComp > 0 ? ((double) totalComp / (totalComp + totalDef)) * 100 : 100.0;
            long completedOrdersCount = orders.stream().filter(o -> "Completed".equalsIgnoreCase(o.getStatus())).count();

            document.add(new Paragraph("Summary Statistics:"));
            document.add(new Paragraph(String.format(" - Orders Completed: %d", completedOrdersCount)));
            document.add(new Paragraph(String.format(" - Total Products Manufactured: %,d", totalComp)));
            document.add(new Paragraph(String.format(" - Defective Products Recorded: %,d", totalDef)));
            document.add(new Paragraph(String.format(" - Factory Quality Efficiency: %.1f%%", efficiency)));
            document.add(new Paragraph(" "));

            // Details Table
            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100f);
            String[] headers = { "Order ID", "Product", "Target", "Good", "Defect", "Status", "Machine", "Operator" };
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Paragraph(header));
                cell.setBackgroundColor(new java.awt.Color(200, 200, 200));
                table.addCell(cell);
            }

            for (ProductionOrder o : orders) {
                table.addCell(o.getId());
                table.addCell(o.getProductId());
                table.addCell(String.valueOf(o.getQuantity()));

                Optional<ProductionTracking> trackOpt = trackingRepository.findByOrderId(o.getId());
                if (trackOpt.isPresent()) {
                    table.addCell(String.valueOf(trackOpt.get().getCompletedQuantity()));
                    table.addCell(String.valueOf(trackOpt.get().getDefectiveQuantity()));
                } else {
                    table.addCell("0");
                    table.addCell("0");
                }

                table.addCell(o.getStatus());
                table.addCell(o.getMachineId() != null ? o.getMachineId() : "-");
                table.addCell(o.getOperatorId() != null ? o.getOperatorId() : "-");
            }

            document.add(table);

            // Add Worker Performance overview to PDF
            Font subtitleFont = new Font(Font.HELVETICA, 14, Font.BOLD);
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Worker Performance Overview:", subtitleFont));
            document.add(new Paragraph(" "));

            PdfPTable workerTable = new PdfPTable(5);
            workerTable.setWidthPercentage(100f);
            String[] workerHeaders = { "Employee ID", "Name", "Completed (Good)", "Defects", "Quality Efficiency %" };
            for (String header : workerHeaders) {
                PdfPCell cell = new PdfPCell(new Paragraph(header));
                cell.setBackgroundColor(new java.awt.Color(200, 200, 200));
                workerTable.addCell(cell);
            }

            List<Employee> operators = employeeRepository.findByRole("OPERATOR");
            for (Employee op : operators) {
                List<ProductionTracking> opTrackings = trackingRepository.findAll().stream()
                        .filter(t -> op.getId().equals(t.getEmployeeId()))
                        .toList();
                int completed = opTrackings.stream().mapToInt(ProductionTracking::getCompletedQuantity).sum();
                int defective = opTrackings.stream().mapToInt(ProductionTracking::getDefectiveQuantity).sum();
                double eff = (completed + defective) > 0 ? ((double) completed / (completed + defective)) * 100 : 100.0;

                workerTable.addCell(op.getId());
                workerTable.addCell(op.getName());
                workerTable.addCell(String.valueOf(completed));
                workerTable.addCell(String.valueOf(defective));
                workerTable.addCell(String.format("%.1f%%", eff));
            }
            document.add(workerTable);

            document.close();

            byte[] bytes = out.toByteArray();

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_PDF);
            responseHeaders.setContentDispositionFormData("attachment", "MES_Production_Report.pdf");

            return ResponseEntity.ok()
                    .headers(responseHeaders)
                    .body(bytes);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
