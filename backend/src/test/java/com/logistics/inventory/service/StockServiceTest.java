package com.logistics.inventory.service;

import com.logistics.inventory.dto.InventoryDtos.StockMovementRequest;
import com.logistics.inventory.entity.Product;
import com.logistics.inventory.entity.StockLevel;
import com.logistics.inventory.entity.StockMovement;
import com.logistics.inventory.entity.Warehouse;
import com.logistics.inventory.exception.BadRequestException;
import com.logistics.inventory.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock private StockLevelRepository stockLevelRepository;
    @Mock private StockMovementRepository stockMovementRepository;
    @Mock private ProductRepository productRepository;
    @Mock private WarehouseRepository warehouseRepository;

    private StockService stockService;

    private Product product;
    private Warehouse warehouse;
    private Warehouse otherWarehouse;

    @BeforeEach
    void setUp() {
        stockService = new StockService(stockLevelRepository, stockMovementRepository,
                productRepository, warehouseRepository);
        product = Product.builder().id(1L).sku("SKU-1").name("Test product").build();
        warehouse = Warehouse.builder().id(10L).code("WH-A").name("A").build();
        otherWarehouse = Warehouse.builder().id(20L).code("WH-B").name("B").build();
    }

    private void givenProductAndWarehouse() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(10L)).thenReturn(Optional.of(warehouse));
    }

    private StockLevel levelWith(int quantity, Warehouse wh) {
        return StockLevel.builder().id(99L).product(product).warehouse(wh).quantity(quantity).build();
    }

    @Test
    void inMovementIncreasesStock() {
        givenProductAndWarehouse();
        StockLevel level = levelWith(5, warehouse);
        when(stockLevelRepository.findByProductIdAndWarehouseId(1L, 10L)).thenReturn(Optional.of(level));

        stockService.recordMovement(new StockMovementRequest(1L, 10L, null,
                StockMovement.Type.IN, 7, null, null), "tester");

        assertThat(level.getQuantity()).isEqualTo(12);
        verify(stockMovementRepository).save(any(StockMovement.class));
    }

    @Test
    void inMovementCreatesLevelWhenMissing() {
        givenProductAndWarehouse();
        when(stockLevelRepository.findByProductIdAndWarehouseId(1L, 10L)).thenReturn(Optional.empty());

        stockService.recordMovement(new StockMovementRequest(1L, 10L, null,
                StockMovement.Type.IN, 4, null, null), "tester");

        ArgumentCaptor<StockLevel> captor = ArgumentCaptor.forClass(StockLevel.class);
        verify(stockLevelRepository).save(captor.capture());
        assertThat(captor.getValue().getQuantity()).isEqualTo(4);
    }

    @Test
    void outMovementDecreasesStock() {
        givenProductAndWarehouse();
        StockLevel level = levelWith(10, warehouse);
        when(stockLevelRepository.findByProductIdAndWarehouseId(1L, 10L)).thenReturn(Optional.of(level));

        stockService.recordMovement(new StockMovementRequest(1L, 10L, null,
                StockMovement.Type.OUT, 6, null, null), "tester");

        assertThat(level.getQuantity()).isEqualTo(4);
    }

    @Test
    void outMovementRejectsInsufficientStock() {
        givenProductAndWarehouse();
        StockLevel level = levelWith(3, warehouse);
        when(stockLevelRepository.findByProductIdAndWarehouseId(1L, 10L)).thenReturn(Optional.of(level));

        assertThatThrownBy(() -> stockService.recordMovement(new StockMovementRequest(1L, 10L, null,
                StockMovement.Type.OUT, 5, null, null), "tester"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Insufficient stock");
        assertThat(level.getQuantity()).isEqualTo(3);
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void transferMovesQuantityBetweenWarehouses() {
        givenProductAndWarehouse();
        when(warehouseRepository.findById(20L)).thenReturn(Optional.of(otherWarehouse));
        StockLevel source = levelWith(10, warehouse);
        StockLevel target = levelWith(2, otherWarehouse);
        when(stockLevelRepository.findByProductIdAndWarehouseId(1L, 10L)).thenReturn(Optional.of(source));
        when(stockLevelRepository.findByProductIdAndWarehouseId(1L, 20L)).thenReturn(Optional.of(target));

        stockService.recordMovement(new StockMovementRequest(1L, 10L, 20L,
                StockMovement.Type.TRANSFER, 4, null, null), "tester");

        assertThat(source.getQuantity()).isEqualTo(6);
        assertThat(target.getQuantity()).isEqualTo(6);
    }

    @Test
    void transferToSameWarehouseIsRejected() {
        givenProductAndWarehouse();

        assertThatThrownBy(() -> stockService.recordMovement(new StockMovementRequest(1L, 10L, 10L,
                StockMovement.Type.TRANSFER, 4, null, null), "tester"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must differ");
    }

    @Test
    void transferRequiresTargetWarehouse() {
        givenProductAndWarehouse();

        assertThatThrownBy(() -> stockService.recordMovement(new StockMovementRequest(1L, 10L, null,
                StockMovement.Type.TRANSFER, 4, null, null), "tester"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Target warehouse");
    }

    @Test
    void nonPositiveQuantityIsRejected() {
        assertThatThrownBy(() -> stockService.recordMovement(new StockMovementRequest(1L, 10L, null,
                StockMovement.Type.IN, 0, null, null), "tester"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void adjustmentSetsAbsoluteQuantity() {
        givenProductAndWarehouse();
        StockLevel level = levelWith(50, warehouse);
        when(stockLevelRepository.findByProductIdAndWarehouseId(1L, 10L)).thenReturn(Optional.of(level));

        stockService.recordMovement(new StockMovementRequest(1L, 10L, null,
                StockMovement.Type.ADJUSTMENT, 17, null, null), "tester");

        assertThat(level.getQuantity()).isEqualTo(17);
    }
}
