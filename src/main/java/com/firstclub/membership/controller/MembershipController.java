package com.firstclub.membership.controller;

import com.firstclub.membership.dto.*;
import com.firstclub.membership.service.MembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.firstclub.membership.model.Plan;
import com.firstclub.membership.model.Tier;
import com.firstclub.membership.model.Benefit;
import java.util.List;
import java.util.Map;
import com.firstclub.membership.repository.PlanRepository;
import com.firstclub.membership.repository.TierRepository;
import com.firstclub.membership.repository.BenefitRepository;
import com.firstclub.membership.repository.TierBenefitRepository;
import com.firstclub.membership.model.TierBenefit;
import com.firstclub.membership.model.User;
import com.firstclub.membership.repository.UserRepository;

@RestController
@RequestMapping("/membership")
@RequiredArgsConstructor
public class MembershipController {
    private final MembershipService membershipService;
    private final PlanRepository planRepository;
    private final TierRepository tierRepository;
    private final BenefitRepository benefitRepository;
    private final TierBenefitRepository tierBenefitRepository;
    private final UserRepository userRepository;

    @PostMapping("/subscribe")
    public ResponseEntity<SubscribeResponse> subscribe(@RequestBody SubscribeRequest request) {
        SubscribeResponse response = membershipService.subscribe(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<MembershipStatusResponse> getStatus(@RequestParam Long userId) {
        MembershipStatusResponse response = membershipService.getStatus(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/upgrade")
    public ResponseEntity<UpgradeResponse> upgrade(@RequestBody UpgradeRequest request) {
        UpgradeResponse response = membershipService.upgrade(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/downgrade")
    public ResponseEntity<DowngradeResponse> downgrade(@RequestBody DowngradeRequest request) {
        DowngradeResponse response = membershipService.downgrade(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cancel")
    public ResponseEntity<CancelResponse> cancel(@RequestBody CancelRequest request) {
        CancelResponse response = membershipService.cancel(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/renew")
    public ResponseEntity<RenewResponse> renew(@RequestBody RenewRequest request) {
        RenewResponse response = membershipService.renew(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/users")
    public ResponseEntity<UserDTO> createUser(@RequestBody CreateUserRequest request) {
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .build();
        user = userRepository.save(user);
        UserDTO userDTO = UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
        return ResponseEntity.ok(userDTO);
    }

    @GetMapping("/plans")
    public ResponseEntity<List<PlanDTO>> getAllPlans() {
        List<PlanDTO> plans = planRepository.findAll().stream().map(plan -> PlanDTO.builder()
                .id(plan.getId())
                .name(plan.getName())
                .price(plan.getPrice())
                .currency(plan.getCurrency())
                .durationMonths(plan.getDurationMonths())
                .isActive(plan.isActive())
                .description(plan.getDescription())
                .defaultTierId(plan.getDefaultTier() != null ? plan.getDefaultTier().getId() : null)
                .defaultTierName(plan.getDefaultTier() != null ? plan.getDefaultTier().getName() : null)
                .build()).toList();
        return ResponseEntity.ok(plans);
    }

    @GetMapping("/tiers")
    public ResponseEntity<List<TierDTO>> getAllTiers() {
        List<TierBenefit> tierBenefits = tierBenefitRepository.findAll();
        List<TierDTO> tiers = tierRepository.findAll().stream().map(tier -> {
            List<BenefitDTO> benefits = tierBenefits.stream()
                    .filter(tb -> tb.getTier().getId().equals(tier.getId()) && tb.isActive())
                    .map(TierBenefit::getBenefit)
                    .map(benefit -> BenefitDTO.builder()
                            .id(benefit.getId())
                            .name(benefit.getName())
                            .description(benefit.getDescription())
                            .type(benefit.getType())
                            .benefitValue(benefit.getBenefitValue())
                            .applicableTo(benefit.getApplicableTo())
                            .build())
                    .toList();
            return TierDTO.builder()
                    .id(tier.getId())
                    .name(tier.getName())
                    .level(tier.getLevel())
                    .criteriaJson(tier.getCriteriaJson())
                    .isActive(tier.isActive())
                    .description(tier.getDescription())
                    .benefits(benefits)
                    .build();
        }).toList();
        return ResponseEntity.ok(tiers);
    }

    @GetMapping("/tiers/{tierId}/benefits")
    public ResponseEntity<List<BenefitDTO>> getBenefitsForTier(@PathVariable Long tierId) {
        List<TierBenefit> tierBenefits = tierBenefitRepository.findAll();
        List<BenefitDTO> benefits = tierBenefits.stream()
                .filter(tb -> tb.getTier().getId().equals(tierId) && tb.isActive())
                .map(TierBenefit::getBenefit)
                .map(benefit -> BenefitDTO.builder()
                        .id(benefit.getId())
                        .name(benefit.getName())
                        .description(benefit.getDescription())
                        .type(benefit.getType())
                        .benefitValue(benefit.getBenefitValue())
                        .applicableTo(benefit.getApplicableTo())
                        .build())
                .toList();
        return ResponseEntity.ok(benefits);
    }

    @GetMapping("/catalog")
    public ResponseEntity<List<CatalogEntryDTO>> getPlanTierCatalog() {
        List<Plan> plans = planRepository.findAll();
        List<Tier> tiers = tierRepository.findAll();
        List<TierBenefit> tierBenefits = tierBenefitRepository.findAll();
        List<CatalogEntryDTO> catalog = new java.util.ArrayList<>();
        for (Plan plan : plans) {
            for (Tier tier : tiers) {
                List<BenefitDTO> benefits = tierBenefits.stream()
                        .filter(tb -> tb.getTier().getId().equals(tier.getId()) && tb.isActive())
                        .map(TierBenefit::getBenefit)
                        .map(benefit -> BenefitDTO.builder()
                                .id(benefit.getId())
                                .name(benefit.getName())
                                .description(benefit.getDescription())
                                .type(benefit.getType())
                                .benefitValue(benefit.getBenefitValue())
                                .applicableTo(benefit.getApplicableTo())
                                .build())
                        .toList();
                CatalogEntryDTO entry = CatalogEntryDTO.builder()
                        .plan(PlanDTO.builder()
                                .id(plan.getId())
                                .name(plan.getName())
                                .price(plan.getPrice())
                                .currency(plan.getCurrency())
                                .durationMonths(plan.getDurationMonths())
                                .isActive(plan.isActive())
                                .description(plan.getDescription())
                                .defaultTierId(plan.getDefaultTier() != null ? plan.getDefaultTier().getId() : null)
                                .defaultTierName(plan.getDefaultTier() != null ? plan.getDefaultTier().getName() : null)
                                .build())
                        .tier(TierDTO.builder()
                                .id(tier.getId())
                                .name(tier.getName())
                                .level(tier.getLevel())
                                .criteriaJson(tier.getCriteriaJson())
                                .isActive(tier.isActive())
                                .description(tier.getDescription())
                                .benefits(benefits)
                                .build())
                        .benefits(benefits)
                        .build();
                catalog.add(entry);
            }
        }
        return ResponseEntity.ok(catalog);
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userRepository.findAll().stream()
                .map(user -> UserDTO.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .build())
                .toList();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(user -> UserDTO.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .build())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id, @RequestBody UserDTO userDTO) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setName(userDTO.getName());
                    user.setEmail(userDTO.getEmail());
                    User updated = userRepository.save(user);
                    return ResponseEntity.ok(UserDTO.builder()
                            .id(updated.getId())
                            .name(updated.getName())
                            .email(updated.getEmail())
                            .build());
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
} 