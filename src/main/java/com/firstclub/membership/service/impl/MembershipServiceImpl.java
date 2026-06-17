package com.firstclub.membership.service.impl;

import com.firstclub.membership.dto.*;
import com.firstclub.membership.repository.*;
import com.firstclub.membership.service.MembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.firstclub.membership.model.*;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MembershipServiceImpl implements MembershipService {
    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final TierRepository tierRepository;
    private final BenefitRepository benefitRepository;
    private final TierBenefitRepository tierBenefitRepository;
    private final MembershipRepository membershipRepository;
    private final TransactionRepository transactionRepository;

    @Override
    public MembershipStatusResponse getStatus(Long userId) {
        MembershipStatusResponse response = new MembershipStatusResponse();
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            response.setStatus("USER_NOT_FOUND");
            return response;
        }
        Optional<Membership> membershipOpt = membershipRepository.findByUserAndStatus(userOpt.get(), "active");
        if (membershipOpt.isEmpty()) {
            response.setStatus("NO_ACTIVE_MEMBERSHIP");
            return response;
        }
        Membership m = membershipOpt.get();
        response.setMembershipId(m.getId());
        response.setPlan(m.getPlan().getName());
        response.setTier(m.getTier().getName());
        response.setStartDate(m.getStartDate().toString());
        response.setExpiryDate(m.getExpiryDate().toString());
        response.setAutoRenew(m.isAutoRenew());
        response.setStatus(m.getStatus());
        return response;
    }

    @Override
    public SubscribeResponse subscribe(SubscribeRequest request) {
        SubscribeResponse response = new SubscribeResponse();
        Optional<User> userOpt = userRepository.findById(request.getUserId());
        if (userOpt.isEmpty()) {
            response.setStatus("USER_NOT_FOUND");
            return response;
        }
        Optional<Plan> planOpt = planRepository.findById(request.getPlanId());
        if (planOpt.isEmpty()) {
            response.setStatus("PLAN_NOT_FOUND");
            return response;
        }
        User user = userOpt.get();
        Plan plan = planOpt.get();
        // Check for existing active membership
        Optional<Membership> existing = membershipRepository.findByUserAndStatus(user, "active");
        if (existing.isPresent()) {
            response.setStatus("ALREADY_SUBSCRIBED");
            return response;
        }
        // Mock payment flow: always succeeds
        // Assign default tier from plan
        Tier tier = plan.getDefaultTier();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiry = now.plusMonths(plan.getDurationMonths());
        Membership membership = Membership.builder()
                .user(user)
                .plan(plan)
                .tier(tier)
                .startDate(now)
                .expiryDate(expiry)
                .autoRenew(request.isAutoRenew())
                .status("active")
                .lastRenewedAt(now)
                .nextRenewalAt(expiry)
                .build();
        membershipRepository.save(membership);
        response.setMembershipId(membership.getId());
        response.setStatus("active");
        response.setStartDate(now.toString());
        response.setExpiryDate(expiry.toString());
        response.setTier(tier.getName());
        return response;
    }

    @Override
    public UpgradeResponse upgrade(UpgradeRequest request) {
        UpgradeResponse response = new UpgradeResponse();
        Optional<User> userOpt = userRepository.findById(request.getUserId());
        if (userOpt.isEmpty()) {
            response.setStatus("USER_NOT_FOUND");
            return response;
        }
        User user = userOpt.get();
        Optional<Membership> membershipOpt = membershipRepository.findByUserAndStatus(user, "active");
        if (membershipOpt.isEmpty()) {
            response.setStatus("NO_ACTIVE_MEMBERSHIP");
            return response;
        }
        Optional<Plan> planOpt = planRepository.findById(request.getTargetPlanId());
        if (planOpt.isEmpty()) {
            response.setStatus("PLAN_NOT_FOUND");
            return response;
        }
        Membership membership = membershipOpt.get();
        Plan newPlan = planOpt.get();
        if (membership.getPlan().getId().equals(newPlan.getId())) {
            response.setStatus("ALREADY_ON_PLAN");
            return response;
        }
        // Mock proration: always succeed, set prorationInfo
        String prorationInfo = "Prorated amount applied.";
        // Mock payment: always succeed
        membership.setPlan(newPlan);
        membership.setTier(newPlan.getDefaultTier());
        membership.setExpiryDate(membership.getStartDate().plusMonths(newPlan.getDurationMonths()));
        membership.setNextRenewalAt(membership.getExpiryDate());
        membershipRepository.save(membership);
        response.setMembershipId(membership.getId());
        response.setStatus("UPGRADED");
        response.setNewTier(newPlan.getDefaultTier().getName());
        response.setProrationInfo(prorationInfo);
        return response;
    }

    @Override
    public DowngradeResponse downgrade(DowngradeRequest request) {
        DowngradeResponse response = new DowngradeResponse();
        Optional<User> userOpt = userRepository.findById(request.getUserId());
        if (userOpt.isEmpty()) {
            response.setStatus("USER_NOT_FOUND");
            return response;
        }
        User user = userOpt.get();
        Optional<Membership> membershipOpt = membershipRepository.findByUserAndStatus(user, "active");
        if (membershipOpt.isEmpty()) {
            response.setStatus("NO_ACTIVE_MEMBERSHIP");
            return response;
        }
        Optional<Plan> planOpt = planRepository.findById(request.getTargetPlanId());
        if (planOpt.isEmpty()) {
            response.setStatus("PLAN_NOT_FOUND");
            return response;
        }
        Membership membership = membershipOpt.get();
        Plan newPlan = planOpt.get();
        if (membership.getPlan().getId().equals(newPlan.getId())) {
            response.setStatus("ALREADY_ON_PLAN");
            return response;
        }
        // Mock proration: always succeed, set prorationInfo
        String prorationInfo = "Prorated amount applied.";
        // Downgrade takes effect at next renewal (simulate by setting plan but not changing expiry)
        membership.setPlan(newPlan);
        membership.setTier(newPlan.getDefaultTier());
        membershipRepository.save(membership);
        response.setMembershipId(membership.getId());
        response.setStatus("DOWNGRADED");
        response.setNewTier(newPlan.getDefaultTier().getName());
        response.setProrationInfo(prorationInfo);
        return response;
    }

    @Override
    public CancelResponse cancel(CancelRequest request) {
        CancelResponse response = new CancelResponse();
        Optional<User> userOpt = userRepository.findById(request.getUserId());
        if (userOpt.isEmpty()) {
            response.setStatus("USER_NOT_FOUND");
            return response;
        }
        User user = userOpt.get();
        Optional<Membership> membershipOpt = membershipRepository.findByUserAndStatus(user, "active");
        if (membershipOpt.isEmpty()) {
            response.setStatus("NO_ACTIVE_MEMBERSHIP");
            return response;
        }
        Membership membership = membershipOpt.get();
        membership.setStatus("cancelled");
        membership.setCancelledAt(LocalDateTime.now());
        // Set grace period (e.g., 7 days)
        membership.setGracePeriodUntil(LocalDateTime.now().plusDays(7));
        membershipRepository.save(membership);
        response.setMembershipId(membership.getId());
        response.setStatus("CANCELLED");
        response.setGracePeriodInfo("Grace period until " + membership.getGracePeriodUntil());
        return response;
    }

    @Override
    public RenewResponse renew(RenewRequest request) {
        RenewResponse response = new RenewResponse();
        Optional<User> userOpt = userRepository.findById(request.getUserId());
        if (userOpt.isEmpty()) {
            response.setStatus("USER_NOT_FOUND");
            return response;
        }
        Optional<Membership> membershipOpt = membershipRepository.findById(request.getMembershipId());
        if (membershipOpt.isEmpty()) {
            response.setStatus("MEMBERSHIP_NOT_FOUND");
            return response;
        }
        Membership membership = membershipOpt.get();
        if (!membership.getUser().getId().equals(request.getUserId())) {
            response.setStatus("MEMBERSHIP_USER_MISMATCH");
            return response;
        }
        // Mock payment: always succeeds
        LocalDateTime newExpiry = membership.getExpiryDate().plusMonths(membership.getPlan().getDurationMonths());
        membership.setExpiryDate(newExpiry);
        membership.setNextRenewalAt(newExpiry);
        membership.setLastRenewedAt(LocalDateTime.now());
        membership.setStatus("active");
        membershipRepository.save(membership);
        response.setMembershipId(membership.getId());
        response.setStatus("RENEWED");
        response.setNewExpiryDate(newExpiry.toString());
        response.setPaymentInfo("Payment successful (mock)");
        return response;
    }
} 