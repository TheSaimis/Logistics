package com.logistics.inventory.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.logistics.inventory.entity.Product;
import com.logistics.inventory.exception.NotFoundException;
import com.logistics.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;

/**
 * Product label barcodes (Code 128) and QR codes rendered server-side with ZXing.
 * Label/scanning workflow inspired by InvenTree and Snipe-IT.
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class BarcodeController {

    private final ProductRepository productRepository;

    @GetMapping(value = "/{id}/barcode", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> barcode(@PathVariable Long id,
                                          @RequestParam(defaultValue = "code128") String format) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Product", id));
        try {
            BitMatrix matrix = "qr".equalsIgnoreCase(format)
                    ? new QRCodeWriter().encode(product.getSku(), BarcodeFormat.QR_CODE, 220, 220)
                    : new Code128Writer().encode(product.getSku(), BarcodeFormat.CODE_128, 320, 90);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(Duration.ofHours(1)))
                    .body(out.toByteArray());
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Failed to render barcode for " + product.getSku(), e);
        }
    }
}
