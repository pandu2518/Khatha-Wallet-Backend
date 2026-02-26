package com.khathabook.model;

@Entity
@Table(name = "feature_flags")
public class FeatureFlag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Default Constructor
    public FeatureFlag() {}

    // All-args Constructor
    public FeatureFlag(Long id, String flagKey, boolean enabled, String description) {
        this.id = id;
        this.flagKey = flagKey;
        this.enabled = enabled;
        this.description = description;
    }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String flagKey;

    @Column(nullable = false)
    private boolean enabled;

    private String description;

    public String getFlagKey() { return flagKey; }
    public void setFlagKey(String flagKey) { this.flagKey = flagKey; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
