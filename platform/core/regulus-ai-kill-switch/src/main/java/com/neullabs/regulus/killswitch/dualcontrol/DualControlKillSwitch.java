package com.neullabs.regulus.killswitch.dualcontrol;

import com.neullabs.regulus.killswitch.interceptor.KillSwitchManager;
import com.neullabs.regulus.killswitch.model.KillSwitchState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dual-control (4-eyes principle) kill switch implementation.
 * Requires two authorized approvers to activate or deactivate critical kill switches.
 *
 * <p>UK Financial Services compliance requirement:
 * <ul>
 *   <li>Critical operations require approval from two independent parties</li>
 *   <li>Prevents single point of failure in governance</li>
 *   <li>Full audit trail of all approval actions</li>
 *   <li>Time-limited pending requests</li>
 * </ul>
 */
public class DualControlKillSwitch {

    private static final Logger log = LoggerFactory.getLogger(DualControlKillSwitch.class);

    private static final Duration DEFAULT_REQUEST_EXPIRY = Duration.ofHours(4);

    private final KillSwitchManager killSwitchManager;
    private final DualControlConfig config;
    private final Map<String, PendingRequest> pendingRequests = new ConcurrentHashMap<>();
    private final List<DualControlAuditEntry> auditLog = Collections.synchronizedList(new ArrayList<>());

    public DualControlKillSwitch(KillSwitchManager killSwitchManager, DualControlConfig config) {
        this.killSwitchManager = killSwitchManager;
        this.config = config;
        log.info("Dual-control kill switch initialized with {} required approvers", config.getRequiredApprovers());
    }

    /**
     * Request global kill switch activation.
     * Requires dual approval if configured.
     *
     * @param reason the reason for activation
     * @param requestedBy the user requesting activation
     * @param emergency if true, bypasses dual control for immediate activation
     * @return the request ID or null if immediately activated (emergency)
     */
    public String requestGlobalActivation(String reason, String requestedBy, boolean emergency) {
        if (emergency && config.isAllowEmergencyBypass()) {
            log.warn("EMERGENCY KILL SWITCH ACTIVATION by {}: {}", requestedBy, reason);
            killSwitchManager.activateGlobal(reason + " [EMERGENCY]", requestedBy);
            recordAudit(null, ActionType.EMERGENCY_ACTIVATE, KillSwitchState.Scope.GLOBAL, null,
                reason, requestedBy, null, true);
            return null;
        }

        if (!config.isDualControlEnabled()) {
            killSwitchManager.activateGlobal(reason, requestedBy);
            return null;
        }

        String requestId = generateRequestId();
        PendingRequest request = new PendingRequest(
            requestId,
            ActionType.ACTIVATE,
            KillSwitchState.Scope.GLOBAL,
            null,
            reason,
            requestedBy,
            Instant.now(),
            config.getRequestExpiry(),
            new HashSet<>(),
            config.getRequiredApprovers()
        );

        request.addApproval(requestedBy); // Requester counts as first approval
        pendingRequests.put(requestId, request);

        log.info("Kill switch activation requested: {} by {} - requires {} more approver(s)",
            requestId, requestedBy, request.remainingApprovals());

        recordAudit(requestId, ActionType.REQUEST_ACTIVATION, KillSwitchState.Scope.GLOBAL, null,
            reason, requestedBy, null, false);

        return requestId;
    }

    /**
     * Request scoped kill switch activation.
     */
    public String requestScopedActivation(KillSwitchState.Scope scope, String targetId,
                                          String reason, String requestedBy, boolean emergency) {
        if (emergency && config.isAllowEmergencyBypass()) {
            log.warn("EMERGENCY SCOPED KILL SWITCH ACTIVATION {}:{} by {}: {}",
                scope, targetId, requestedBy, reason);
            killSwitchManager.activateScoped(scope, targetId, reason + " [EMERGENCY]", requestedBy);
            recordAudit(null, ActionType.EMERGENCY_ACTIVATE, scope, targetId,
                reason, requestedBy, null, true);
            return null;
        }

        if (!config.isDualControlEnabled()) {
            killSwitchManager.activateScoped(scope, targetId, reason, requestedBy);
            return null;
        }

        String requestId = generateRequestId();
        PendingRequest request = new PendingRequest(
            requestId,
            ActionType.ACTIVATE,
            scope,
            targetId,
            reason,
            requestedBy,
            Instant.now(),
            config.getRequestExpiry(),
            new HashSet<>(),
            config.getRequiredApprovers()
        );

        request.addApproval(requestedBy);
        pendingRequests.put(requestId, request);

        log.info("Scoped kill switch activation requested: {} {}:{} by {}",
            requestId, scope, targetId, requestedBy);

        return requestId;
    }

    /**
     * Request kill switch deactivation.
     */
    public String requestDeactivation(KillSwitchState.Scope scope, String targetId,
                                      String requestedBy) {
        if (!config.isDualControlEnabled()) {
            if (scope == KillSwitchState.Scope.GLOBAL) {
                killSwitchManager.deactivateGlobal(requestedBy);
            } else {
                killSwitchManager.deactivateScoped(scope, targetId, requestedBy);
            }
            return null;
        }

        String requestId = generateRequestId();
        PendingRequest request = new PendingRequest(
            requestId,
            ActionType.DEACTIVATE,
            scope,
            targetId,
            "Deactivation request",
            requestedBy,
            Instant.now(),
            config.getRequestExpiry(),
            new HashSet<>(),
            config.getRequiredApprovers()
        );

        request.addApproval(requestedBy);
        pendingRequests.put(requestId, request);

        log.info("Kill switch deactivation requested: {} by {}", requestId, requestedBy);
        return requestId;
    }

    /**
     * Approve a pending request.
     *
     * @param requestId the request ID
     * @param approver the approving user
     * @return true if the request was approved and executed
     */
    public boolean approve(String requestId, String approver) {
        PendingRequest request = pendingRequests.get(requestId);

        if (request == null) {
            log.warn("Approval attempted for unknown request: {}", requestId);
            return false;
        }

        if (request.isExpired()) {
            log.warn("Approval attempted for expired request: {}", requestId);
            pendingRequests.remove(requestId);
            recordAudit(requestId, ActionType.EXPIRED, request.scope, request.targetId,
                request.reason, request.requestedBy, null, false);
            return false;
        }

        if (!isAuthorizedApprover(approver)) {
            log.warn("Unauthorized approval attempt by {} for request {}", approver, requestId);
            recordAudit(requestId, ActionType.UNAUTHORIZED_APPROVAL, request.scope, request.targetId,
                request.reason, request.requestedBy, approver, false);
            return false;
        }

        if (request.requestedBy.equals(approver) && !config.isAllowSelfApproval()) {
            log.warn("Self-approval not allowed: {} for request {}", approver, requestId);
            return false;
        }

        request.addApproval(approver);

        log.info("Approval received for request {}: {} ({}/{} approvals)",
            requestId, approver, request.approvals.size(), request.requiredApprovals);

        if (request.isFullyApproved()) {
            executeRequest(request);
            pendingRequests.remove(requestId);
            return true;
        }

        return false;
    }

    /**
     * Reject a pending request.
     */
    public void reject(String requestId, String rejector, String reason) {
        PendingRequest request = pendingRequests.remove(requestId);

        if (request != null) {
            log.info("Request {} rejected by {}: {}", requestId, rejector, reason);
            recordAudit(requestId, ActionType.REJECTED, request.scope, request.targetId,
                reason, request.requestedBy, rejector, false);
        }
    }

    /**
     * Get all pending requests.
     */
    public List<PendingRequest> getPendingRequests() {
        cleanupExpiredRequests();
        return new ArrayList<>(pendingRequests.values());
    }

    /**
     * Get the audit log.
     */
    public List<DualControlAuditEntry> getAuditLog() {
        return new ArrayList<>(auditLog);
    }

    /**
     * Get the audit log for a specific request.
     */
    public List<DualControlAuditEntry> getAuditLogForRequest(String requestId) {
        return auditLog.stream()
            .filter(e -> requestId.equals(e.requestId))
            .toList();
    }

    private void executeRequest(PendingRequest request) {
        if (request.actionType == ActionType.ACTIVATE) {
            if (request.scope == KillSwitchState.Scope.GLOBAL) {
                killSwitchManager.activateGlobal(request.reason, "dual-control:" + request.requestedBy);
            } else {
                killSwitchManager.activateScoped(request.scope, request.targetId,
                    request.reason, "dual-control:" + request.requestedBy);
            }
        } else {
            if (request.scope == KillSwitchState.Scope.GLOBAL) {
                killSwitchManager.deactivateGlobal("dual-control:" + request.requestedBy);
            } else {
                killSwitchManager.deactivateScoped(request.scope, request.targetId,
                    "dual-control:" + request.requestedBy);
            }
        }

        recordAudit(request.requestId, request.actionType, request.scope, request.targetId,
            request.reason, request.requestedBy, String.join(",", request.approvals), true);

        log.info("Kill switch request {} executed with approvals: {}", request.requestId, request.approvals);
    }

    private boolean isAuthorizedApprover(String userId) {
        if (config.getAuthorizedApprovers().isEmpty()) {
            return true; // No whitelist means anyone can approve
        }
        return config.getAuthorizedApprovers().contains(userId);
    }

    private void cleanupExpiredRequests() {
        List<String> expired = new ArrayList<>();
        for (Map.Entry<String, PendingRequest> entry : pendingRequests.entrySet()) {
            if (entry.getValue().isExpired()) {
                expired.add(entry.getKey());
            }
        }
        for (String key : expired) {
            PendingRequest request = pendingRequests.remove(key);
            recordAudit(key, ActionType.EXPIRED, request.scope, request.targetId,
                request.reason, request.requestedBy, null, false);
        }
    }

    private String generateRequestId() {
        return "KS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void recordAudit(String requestId, ActionType action, KillSwitchState.Scope scope,
                            String targetId, String reason, String requestedBy, String approvers,
                            boolean executed) {
        auditLog.add(new DualControlAuditEntry(
            UUID.randomUUID().toString(),
            requestId,
            action,
            scope,
            targetId,
            reason,
            requestedBy,
            approvers,
            executed,
            Instant.now()
        ));
    }

    /**
     * Action type for audit logging.
     */
    public enum ActionType {
        REQUEST_ACTIVATION,
        REQUEST_DEACTIVATION,
        ACTIVATE,
        DEACTIVATE,
        EMERGENCY_ACTIVATE,
        REJECTED,
        EXPIRED,
        UNAUTHORIZED_APPROVAL
    }

    /**
     * Pending request record.
     */
    public static class PendingRequest {
        private final String requestId;
        private final ActionType actionType;
        private final KillSwitchState.Scope scope;
        private final String targetId;
        private final String reason;
        private final String requestedBy;
        private final Instant requestedAt;
        private final Duration expiry;
        private final Set<String> approvals;
        private final int requiredApprovals;

        public PendingRequest(String requestId, ActionType actionType, KillSwitchState.Scope scope,
                             String targetId, String reason, String requestedBy, Instant requestedAt,
                             Duration expiry, Set<String> approvals, int requiredApprovals) {
            this.requestId = requestId;
            this.actionType = actionType;
            this.scope = scope;
            this.targetId = targetId;
            this.reason = reason;
            this.requestedBy = requestedBy;
            this.requestedAt = requestedAt;
            this.expiry = expiry;
            this.approvals = approvals;
            this.requiredApprovals = requiredApprovals;
        }

        public void addApproval(String approver) {
            approvals.add(approver);
        }

        public boolean isExpired() {
            return Instant.now().isAfter(requestedAt.plus(expiry));
        }

        public boolean isFullyApproved() {
            return approvals.size() >= requiredApprovals;
        }

        public int remainingApprovals() {
            return Math.max(0, requiredApprovals - approvals.size());
        }

        // Getters
        public String getRequestId() { return requestId; }
        public ActionType getActionType() { return actionType; }
        public KillSwitchState.Scope getScope() { return scope; }
        public String getTargetId() { return targetId; }
        public String getReason() { return reason; }
        public String getRequestedBy() { return requestedBy; }
        public Instant getRequestedAt() { return requestedAt; }
        public Set<String> getApprovals() { return Set.copyOf(approvals); }
        public int getRequiredApprovals() { return requiredApprovals; }
    }

    /**
     * Audit entry for dual-control actions.
     */
    public record DualControlAuditEntry(
        String auditId,
        String requestId,
        ActionType action,
        KillSwitchState.Scope scope,
        String targetId,
        String reason,
        String requestedBy,
        String approvers,
        boolean executed,
        Instant timestamp
    ) {}

    /**
     * Configuration for dual-control kill switch.
     */
    public static class DualControlConfig {
        private boolean dualControlEnabled = true;
        private int requiredApprovers = 2;
        private boolean allowEmergencyBypass = true;
        private boolean allowSelfApproval = false;
        private Duration requestExpiry = DEFAULT_REQUEST_EXPIRY;
        private List<String> authorizedApprovers = List.of();

        public boolean isDualControlEnabled() { return dualControlEnabled; }
        public void setDualControlEnabled(boolean dualControlEnabled) { this.dualControlEnabled = dualControlEnabled; }
        public int getRequiredApprovers() { return requiredApprovers; }
        public void setRequiredApprovers(int requiredApprovers) { this.requiredApprovers = requiredApprovers; }
        public boolean isAllowEmergencyBypass() { return allowEmergencyBypass; }
        public void setAllowEmergencyBypass(boolean allowEmergencyBypass) { this.allowEmergencyBypass = allowEmergencyBypass; }
        public boolean isAllowSelfApproval() { return allowSelfApproval; }
        public void setAllowSelfApproval(boolean allowSelfApproval) { this.allowSelfApproval = allowSelfApproval; }
        public Duration getRequestExpiry() { return requestExpiry; }
        public void setRequestExpiry(Duration requestExpiry) { this.requestExpiry = requestExpiry; }
        public List<String> getAuthorizedApprovers() { return authorizedApprovers; }
        public void setAuthorizedApprovers(List<String> authorizedApprovers) { this.authorizedApprovers = authorizedApprovers; }
    }
}
