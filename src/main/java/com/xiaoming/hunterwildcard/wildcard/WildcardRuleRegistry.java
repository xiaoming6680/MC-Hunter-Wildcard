package com.xiaoming.hunterwildcard.wildcard;

import com.xiaoming.hunterwildcard.wildcard.rules.BlockDecayRule;
import com.xiaoming.hunterwildcard.wildcard.rules.BloodRageRule;
import com.xiaoming.hunterwildcard.wildcard.rules.CompassChaosRule;
import com.xiaoming.hunterwildcard.wildcard.rules.DisabledWildcardRule;
import com.xiaoming.hunterwildcard.wildcard.rules.ExplosiveDeathRule;
import com.xiaoming.hunterwildcard.wildcard.rules.FeatherweightRule;
import com.xiaoming.hunterwildcard.wildcard.rules.GlowingRule;
import com.xiaoming.hunterwildcard.wildcard.rules.HungerChaseRule;
import com.xiaoming.hunterwildcard.wildcard.rules.HunterRadarRule;
import com.xiaoming.hunterwildcard.wildcard.rules.LightLoadRule;
import com.xiaoming.hunterwildcard.wildcard.rules.NightHuntRule;
import com.xiaoming.hunterwildcard.wildcard.rules.PearlFrenzyRule;
import com.xiaoming.hunterwildcard.wildcard.rules.SpeedRushRule;
import com.xiaoming.hunterwildcard.wildcard.rules.SupplyDropRule;
import com.xiaoming.hunterwildcard.wildcard.rules.WeaponOverheatRule;
import com.xiaoming.hunterwildcard.wildcard.rules.WindChargeBrawlRule;

import java.util.List;

final class WildcardRuleRegistry {
    private WildcardRuleRegistry() {
    }

    static List<WildcardRule> createRegisteredRules() {
        return List.of(
                new SpeedRushRule(),
                new FeatherweightRule(),
                new GlowingRule(),
                new NightHuntRule(),
                new ExplosiveDeathRule(),
                new SupplyDropRule(),
                new HunterRadarRule(),
                new CompassChaosRule(),
                new HungerChaseRule(),
                new WeaponOverheatRule(),
                new LightLoadRule(),
                new BlockDecayRule(),
                new PearlFrenzyRule(),
                new WindChargeBrawlRule(),
                new BloodRageRule(),
                new DisabledWildcardRule()
        );
    }
}
