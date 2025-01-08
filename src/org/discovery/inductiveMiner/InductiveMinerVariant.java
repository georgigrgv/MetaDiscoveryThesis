package org.discovery.inductiveMiner;

import org.processmining.plugins.InductiveMiner.plugins.dialogs.IMMiningDialog;

public enum InductiveMinerVariant {
    CLASSIC("Inductive Miner (IM)"),
    INFREQUENT("Inductive Miner - infrequent (IMf)"),
    ALL_OPERATORS("Inductive Miner - all operators (IMa)"),
    INFREQUENT_ALL_OPERATORS("Inductive Miner - infrequent & all operators (IMfa)"),
    INCOMPLETENESS("Inductive Miner - incompleteness (IMc)"),
    EXHAUSTIVE("Inductive Miner - exhaustive K-successor"),
    LIFE_CYCLE("Inductive Miner - life cycle (IMlc)"),
    INFREQUENT_LIFE_CYCLE("Inductive Miner - infrequent & life cycle (IMflc)");

    private final String name;

    InductiveMinerVariant(final String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }

    private static InductiveMinerVariant fromString(String name) {
        for (InductiveMinerVariant variant : values()) {
            if (variant.name.equals(name)) {
                return variant;
            }
        }
        throw new IllegalArgumentException("Unknown variant: " + name);
    }

    public static IMMiningDialog.Variant variant(String name, IMMiningDialog dialog) {
        try {
            InductiveMinerVariant variant = fromString(name);
            switch (variant) {
                case CLASSIC:
                    return dialog.new VariantIM();
                case INFREQUENT:
                    return dialog.new VariantIMf();
                case ALL_OPERATORS:
                    return dialog.new VariantIMa();
                case INFREQUENT_ALL_OPERATORS:
                    return dialog.new VariantIMfa();
                case INCOMPLETENESS:
                    return dialog.new VariantIMc();
                case EXHAUSTIVE:
                    return dialog.new VariantIMEKS();
                case LIFE_CYCLE:
                    return dialog.new VariantIMlc();
                case INFREQUENT_LIFE_CYCLE:
                    return dialog.new VariantIMflc();
                default:
                    throw new IllegalArgumentException("Unhandled variant: " + variant);
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
        }
        return null;
    }
}
