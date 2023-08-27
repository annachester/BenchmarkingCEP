# Pattern Catalog

#### Pattern SEQ1-SP0
```
PATTERN SEQ(V v1, Q q1)
WHERE v1.value > velFilter && q1.value > quaFilter && distance(q1,v1) < 10.0 (km)
WITHIN Minutes(windowSize)
WITH non-deterministic relaxed contiguity

class-number: Q1
```

#### Pattern SEQ1-SP1
```
PATTERN SEQ(V v1, Q q1)
WHERE v1.value > velFilter && q1.value > quaFilter && distance(q1,v1) < 10.0 (km)
WITHIN Minutes(windowSize)
WITH relaxed contiguity

class-number: Q1_1
```

#### Pattern SEQ1-SP2
```
PATTERN SEQ(V v1, Q q1)
WHERE v1.value > velFilter && q1.value > quaFilter && distance(q1,v1) < 10.0 (km)
WITHIN Minutes(windowSize)
WITH strict contiguity

class-number: Q1_2
```


#### Pattern SEQ2-SP0

```
SEQ2: This pattern looks for a sequence of two consecutive V events where the velocity values are above the threshold
(velFilter) and the velocity values increase. Followed by a quantity event below the quantity threshold and
and where the quantity values decrease.
Performed With strict contiguity, relaxed contiguity and non-deterministic relaxed contiguity.
```
```
Pattern SEQ(V v1, Q q1, V v2, Q q2)
WHERE v1.value > velFilter && q1.value < quaFilter && v2.value > velFilter && q2.value < quaFilter
&& v2.value >= v1.value && q2.value <= q1.value
WITHIN Minutes(windowSize)
WITH non-deterministic relaxed contiguity

class-number: Q8
```

#### Pattern SEQ2-SP1
```
Pattern SEQ(V v1, Q q1, V v2, Q q2)
WHERE v1.value > velFilter && q1.value < quaFilter && v2.value > velFilter && q2.value < quaFilter
&& v2.value >= v1.value && q2.value <= q1.value
WITHIN Minutes(windowSize)
WITH relaxed contiguity

class-number: Q1_1
```

#### Pattern SEQ2-SP2
```
Pattern SEQ(V v1, Q q1, V v2, Q q2)
WHERE v1.value > velFilter && q1.value < quaFilter && v2.value > velFilter && q2.value < quaFilter
&& v2.value >= v1.value && q2.value <= q1.value
WITHIN Minutes(windowSize)
WITH stric contiguity

class-number: Q1_2
```

#### Pattern AND
```
PATTERN AND(V v1, Q q1)
WHERE v1. value > velFilter && q1. value > quaFilter && distance(q1,v1) < 10.0 (km)
WITHIN Minutes(windowSize)

class-number: Q2
```

#### Pattern OR
```
PATTERN OR(V v1, Q q1)
WHERE v1. value > velFilter && q1. value > quaFilter
WITHIN Minutes(windowSize)

class-number: Q3
```

#### Pattern ITER1
``` 
PATTERN V v1[n]
WHERE v[i].value > v[i-1].value 
WITHIN Minutes(windowSize) 

class-number: Q6
```

#### Pattern ITER2
``` 
PATTERN V v1[n]
WHERE v[i].value >  velFilter
WITHIN Minutes(windowSize) 

class-number: Q7
```


```
other sequence pattern ideas that could make sense for this use case, but not implemented:
```
#### Pattern SEQ3
```
Quantity Decrease Pattern:
Pattern SEQ(Q q1, Q q2, Q q3)
WHERE q1.value > quaFilter && q2.value > quaFilter && q3.value > quaFilter
&& q1.value >= q2.value && q2.value >= q3.value
WITHIN Minutes(windowSize)
WITH strict contiguity
(WITH relaxed contiguity)
(WITH non-deterministic relaxed contiguity)

This pattern looks for a sequence of three consecutive Q events where the quantity values are above the threshold
and the quantity values steadily decrease.
```

#### Pattern SEQ4
```
Velocity Spike Pattern:
Pattern SEQ(V v1, V v2, V v3)
WHERE v1.value <= velThreshold && v2.value > velThreshold*1.5 && v3.value <= velThreshold
WITHIN Minutes(windowSize)
WITH strict contiguity
(WITH relaxed contiguity)
(WITH non-deterministic relaxed contiguity)

This pattern looks for a sequence of three V events where the velocity values start below a threshold, 
then spike above the threshold (+50%), and return below the threshold again. 
-> sudden increases in traffic speed or acceleration.
```

#### Pattern SEQ5
```
Velocity and Quantity Inverse Pattern:
Pattern SEQ(V v, Q q)
WHERE v.value > velFilter && q.value > quaFilter && v.value > (2 * q.value)
WITHIN Minutes(windowSize)
WITH strict contiguity
(WITH relaxed contiguity)
(WITH non-deterministic relaxed contiguity)

This pattern looks for a sequence of a V event followed by a Q event where both the velocity and quantity values
are above their respective thresholds and the velocity value is at least twice the quantity value.
```

#### Pattern SEQ6
```
Velocity Change and Quantity Change Pattern:
Pattern SEQ(V v1, Q q1, V v2, Q q2)
WHERE v1.value > velFilter && q1.value > quaFilter && v2.value > velFilter && q2.value > quaFilter
&& (v2.value - v1.value) >= velChangeThreshold && (q2.value - q1.value) >= quaChangeThreshold
WITHIN Minutes(windowSize)
WITH strict contiguity
(WITH relaxed contiguity)
(WITH non-deterministic relaxed contiguity)

This pattern looks for a sequence of two consecutive V-Q pairs where the velocity and quantity values increase
by a certain threshold between the pairs.
```

#### Pattern SEQ7
```
Traffic Flow Change Pattern:
Pattern SEQ(Q q1, Q q2, V v)
WHERE q1.value > quaThreshold && q2.value > quaThreshold && v.value > velThreshold
&& q2.value - q1.value >= quaChangeThreshold
WITHIN Minutes(windowSize)
WITH strict contiguity
(WITH relaxed contiguity)
(WITH non-deterministic relaxed contiguity)

This pattern looks for a sequence of two consecutive Q events followed by a V event where the quantity values
remain above a threshold indicating traffic flow, and there is a significant change in the quantity values
between the two events. -> identify changes in traffic flow patterns
```

#### Pattern SEQ8
```
Peak Traffic Hours Pattern:
Pattern SEQ(Q q1, V v1, Q q2, V v2)
WHERE q1.value > quaThreshold && v1.value > velThreshold && q2.value > quaThreshold && v2.value > velThreshold
&& (v2.value - v1.value) <= velChangeThreshold
WITHIN Hours(windowSize)
WITH strict contiguity
(WITH relaxed contiguity)
(WITH non-deterministic relaxed contiguity)

This pattern looks for a sequence of two Q-V pairs where the quantity and velocity values are above thresholds
indicating regular traffic flow, and there is no significant change in the velocity values between the pairs.
-> peak traffic hours when traffic conditions are relatively stable.
```