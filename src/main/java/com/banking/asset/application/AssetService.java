package com.banking.asset.application;

import com.banking.asset.api.dto.AssetRequest;
import com.banking.asset.api.dto.AssetResponse;
import com.banking.asset.domain.CustomerAsset;
import com.banking.asset.infrastructure.CustomerAssetRepository;
import com.banking.shared.error.ResourceNotFoundException;
import com.banking.shared.security.SecurityUtils;
import com.banking.user.domain.User;
import com.banking.user.infrastructure.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssetService {
  private final CustomerAssetRepository assetRepository;
  private final UserRepository userRepository;

  public AssetService(CustomerAssetRepository assetRepository, UserRepository userRepository) {
    this.assetRepository = assetRepository;
    this.userRepository = userRepository;
  }

  public List<AssetResponse> list() {
    return assetRepository
        .findByUserEmailOrderByUpdatedAtDesc(SecurityUtils.getCurrentUserEmail())
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public AssetResponse create(AssetRequest r) {
    User user =
        userRepository
            .findByEmail(SecurityUtils.getCurrentUserEmail())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    CustomerAsset asset = new CustomerAsset();
    asset.setUser(user);
    applyChanges(asset, r);
    return toResponse(assetRepository.save(asset));
  }

  @Transactional
  public AssetResponse update(Long id, AssetRequest r) {
    CustomerAsset asset = getOwnedAsset(id);
    applyChanges(asset, r);
    return toResponse(assetRepository.save(asset));
  }

  @Transactional
  public void delete(Long id) {
    assetRepository.delete(getOwnedAsset(id));
  }

  private CustomerAsset getOwnedAsset(Long id) {
    return assetRepository
        .findByIdAndUserEmail(id, SecurityUtils.getCurrentUserEmail())
        .orElseThrow(() -> new ResourceNotFoundException("Asset not found"));
  }

  private void applyChanges(CustomerAsset asset, AssetRequest request) {
    asset.setAssetType(request.assetType());
    asset.setAssetName(request.assetName().trim());
    asset.setEstimatedValue(request.estimatedValue());
    asset.setCurrency(request.currency());
    asset.setDescription(request.description());
  }

  private AssetResponse toResponse(CustomerAsset asset) {
    return new AssetResponse(
        asset.getId(),
        asset.getAssetType(),
        asset.getAssetName(),
        asset.getEstimatedValue(),
        asset.getCurrency(),
        asset.getDescription(),
        asset.getUpdatedAt());
  }
}
