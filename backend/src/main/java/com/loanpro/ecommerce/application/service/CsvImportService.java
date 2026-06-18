package com.loanpro.ecommerce.application.service;

import com.loanpro.ecommerce.application.dto.CsvImportResult;
import com.loanpro.ecommerce.application.exception.InvalidCsvException;
import com.loanpro.ecommerce.domain.entity.Category;
import com.loanpro.ecommerce.domain.entity.Product;
import com.loanpro.ecommerce.domain.repository.CategoryRepository;
import com.loanpro.ecommerce.domain.repository.ProductRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class CsvImportService {

    private static final List<String> EXPECTED_HEADERS = List.of(
            "name", "sku", "description", "category", "price", "stock", "weight_kg"
    );
    private static final long ERROR_FILE_TTL_MS = 10 * 60 * 1000;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ConcurrentHashMap<String, ErrorFileEntry> errorFileStore = new ConcurrentHashMap<>();

    public CsvImportService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public CsvImportResult importProducts(MultipartFile file) {
        validateFile(file);

        List<String> lines = readLines(file);
        if (lines.isEmpty()) {
            throw new InvalidCsvException("file is empty");
        }

        String headerLine = lines.get(0);
        int[] columnMapping = validateAndMapHeader(headerLine);

        List<String> dataLines = lines.subList(1, lines.size());
        if (dataLines.isEmpty()) {
            throw new InvalidCsvException("file is empty");
        }

        boolean allBlank = dataLines.stream().allMatch(l -> l.isBlank());
        if (allBlank) {
            throw new InvalidCsvException("file is empty");
        }

        Set<String> existingSkus = productRepository.findAll().stream()
                .map(Product::getSku)
                .collect(Collectors.toSet());

        Map<String, Category> categoryCache = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getName, c -> c));

        Set<String> csvSeenSkus = new HashSet<>();
        int totalRows = 0;
        int successCount = 0;
        int errorCount = 0;
        List<String[]> errorRows = new ArrayList<>();
        int columnCount = EXPECTED_HEADERS.size();

        for (String line : dataLines) {
            if (line.isBlank()) {
                continue;
            }

            totalRows++;
            String[] fields = parseCsvLine(line);

            if (fields.length != columnCount) {
                String[] errorRow = new String[columnCount + 1];
                System.arraycopy(fields, 0, errorRow, 0, Math.min(fields.length, columnCount));
                errorRow[columnCount] = "row has incorrect number of columns";
                errorRows.add(errorRow);
                errorCount++;
                continue;
            }

            String[] mapped = new String[columnCount];
            for (int i = 0; i < columnCount; i++) {
                mapped[i] = fields[columnMapping[i]];
            }

            List<String> errors = new ArrayList<>();

            String name = validateName(mapped[0], errors);
            String sku = validateSku(mapped[1], errors, csvSeenSkus, existingSkus);
            String description = trimToNull(mapped[2]);
            String categoryName = validateCategory(mapped[3], errors);
            BigDecimal price = validatePrice(mapped[4], errors);
            Integer stock = validateStock(mapped[5], errors);
            BigDecimal weightKg = validateWeightKg(mapped[6], errors);

            if (!errors.isEmpty()) {
                String[] errorRow = new String[columnCount + 1];
                System.arraycopy(mapped, 0, errorRow, 0, columnCount);
                errorRow[columnCount] = String.join("; ", errors);
                errorRows.add(errorRow);
                errorCount++;
                continue;
            }

            Category category = categoryCache.computeIfAbsent(categoryName,
                    n -> categoryRepository.save(Category.builder().name(n).build()));

            Product product = Product.builder()
                    .name(name)
                    .sku(sku)
                    .description(description)
                    .category(category)
                    .price(price)
                    .stock(stock)
                    .weightKg(weightKg)
                    .build();

            productRepository.save(product);
            existingSkus.add(sku);
            successCount++;
        }

        CsvImportResult.CsvImportResultBuilder resultBuilder = CsvImportResult.builder()
                .totalRows(totalRows)
                .successCount(successCount)
                .errorCount(errorCount);

        if (!errorRows.isEmpty()) {
            byte[] errorCsvBytes = generateErrorCsv(errorRows);
            String errorFileId = UUID.randomUUID().toString();
            errorFileStore.put(errorFileId, new ErrorFileEntry(errorCsvBytes, Instant.now()));
            resultBuilder.errorCsvBytes(errorCsvBytes).errorFileId(errorFileId);
        }

        return resultBuilder.build();
    }

    public byte[] getErrorFile(String errorFileId) {
        ErrorFileEntry entry = errorFileStore.get(errorFileId);
        if (entry == null) {
            return null;
        }
        return entry.data;
    }

    @Scheduled(fixedRate = 60000)
    public void cleanupExpiredErrorFiles() {
        Instant cutoff = Instant.now().minusMillis(ERROR_FILE_TTL_MS);
        errorFileStore.entrySet().removeIf(e -> e.getValue().createdAt.isBefore(cutoff));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidCsvException("file is empty");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".csv")) {
            throw new InvalidCsvException("invalid file type, only .csv allowed");
        }
    }

    private List<String> readLines(MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.toList());
        } catch (IOException e) {
            throw new InvalidCsvException("failed to read file");
        }
    }

    private int[] validateAndMapHeader(String headerLine) {
        String[] headers = parseCsvLine(headerLine);
        List<String> headerList = Arrays.stream(headers)
                .map(h -> h.trim().toLowerCase())
                .collect(Collectors.toList());

        if (headerList.size() != EXPECTED_HEADERS.size()) {
            throw new InvalidCsvException("invalid CSV header");
        }

        Set<String> headerSet = new HashSet<>(headerList);
        if (!headerSet.equals(new HashSet<>(EXPECTED_HEADERS))) {
            throw new InvalidCsvException("invalid CSV header");
        }

        int[] mapping = new int[EXPECTED_HEADERS.size()];
        for (int i = 0; i < EXPECTED_HEADERS.size(); i++) {
            mapping[i] = headerList.indexOf(EXPECTED_HEADERS.get(i));
        }
        return mapping;
    }

    String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    fields.add(current.toString());
                    current = new StringBuilder();
                } else {
                    current.append(c);
                }
            }
        }
        fields.add(current.toString());

        return fields.toArray(new String[0]);
    }

    private String validateName(String raw, List<String> errors) {
        if (raw == null || raw.trim().isEmpty()) {
            errors.add("name is required");
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.length() > 200) {
            errors.add("name exceeds 200 characters");
            return null;
        }
        return trimmed;
    }

    private String validateSku(String raw, List<String> errors, Set<String> csvSeenSkus, Set<String> existingSkus) {
        if (raw == null || raw.trim().isEmpty()) {
            errors.add("sku is required");
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.length() > 50) {
            errors.add("sku exceeds 50 characters");
            return null;
        }
        if (csvSeenSkus.contains(trimmed)) {
            errors.add("duplicate sku in CSV");
            return null;
        }
        if (existingSkus.contains(trimmed)) {
            errors.add("sku already exists in database");
            return null;
        }
        csvSeenSkus.add(trimmed);
        return trimmed;
    }

    private String validateCategory(String raw, List<String> errors) {
        if (raw == null || raw.trim().isEmpty()) {
            errors.add("category is required");
            return null;
        }
        return raw.trim();
    }

    private BigDecimal validatePrice(String raw, List<String> errors) {
        if (raw == null || raw.trim().isEmpty()) {
            errors.add("price is required");
            return null;
        }
        String cleaned = raw.trim().replace("$", "").trim();
        try {
            BigDecimal price = new BigDecimal(cleaned);
            if (price.compareTo(BigDecimal.ZERO) < 0) {
                errors.add("price must be >= 0");
                return null;
            }
            return price.setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            errors.add("price must be a valid number");
            return null;
        }
    }

    private Integer validateStock(String raw, List<String> errors) {
        if (raw == null || raw.trim().isEmpty()) {
            errors.add("stock is required");
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.contains(".")) {
            errors.add("stock must be a valid integer");
            return null;
        }
        try {
            int stock = Integer.parseInt(trimmed);
            if (stock < 0) {
                errors.add("stock must be >= 0");
                return null;
            }
            return stock;
        } catch (NumberFormatException e) {
            errors.add("stock must be a valid integer");
            return null;
        }
    }

    private BigDecimal validateWeightKg(String raw, List<String> errors) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String trimmed = raw.trim();
        try {
            BigDecimal weight = new BigDecimal(trimmed);
            if (weight.compareTo(BigDecimal.ZERO) <= 0) {
                errors.add("weight_kg must be > 0");
                return null;
            }
            return weight;
        } catch (NumberFormatException e) {
            errors.add("weight_kg must be a valid number");
            return null;
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private byte[] generateErrorCsv(List<String[]> errorRows) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            String header = String.join(",", EXPECTED_HEADERS) + ",error_reason\n";
            baos.write(header.getBytes(StandardCharsets.UTF_8));
            for (String[] row : errorRows) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < row.length; i++) {
                    if (i > 0) {
                        sb.append(',');
                    }
                    sb.append(escapeCsvField(row[i]));
                }
                sb.append('\n');
                baos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new RuntimeException("failed to generate error CSV", e);
        }
        return baos.toByteArray();
    }

    private String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    private record ErrorFileEntry(byte[] data, Instant createdAt) {}
}
