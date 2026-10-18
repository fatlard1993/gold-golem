# Gold Golem

A golem built of gold, the way an iron one is built of iron. It does not fight for you: it keeps you alive. Every few seconds it mends whoever is hurt around it, paid for out of gilt that slowly comes back by itself and quickly with gold you feed it, and piglins near one leave you alone as if you were wearing gold yourself.

## What This Mod Does

The game has a golem that fights, a golem that throws snowballs and a golem that sorts chests. Nothing in it looks after anyone. This adds the one that does, out of the metal the game already ties to healing: golden apples, golden carrots, and the gold piglins will trade their tempers for.

Server-side; Pandorical carries the client's half.

## Building One

Exactly like an iron golem, in **blocks of gold**: two stacked, one more on each side of the top one, and a carved pumpkin or jack o'lantern on top, placed last. The blocks vanish and the golem stands where they were. A dispenser facing the spot can place the pumpkin, the same as for an iron golem.

That is four blocks of gold, thirty-six ingots. It is meant to be something you build once for a base or a village, not something you keep a stack of.

## What It Does

| | |
|---|---|
| **Mends** | Every five seconds, everyone hurt within six blocks gets Regeneration II for six seconds |
| **Who counts** | Players, villagers and wandering traders, golems of every kind, and any animal someone has tamed. Never a monster, and never whatever it is fighting |
| **Walks to the hurt** | Anyone hurt within sixteen blocks, while it has gilt to spend |
| **Keeps piglins calm** | Within sixteen blocks of one, piglins treat you as wearing gold |
| **Fights** | Only what hits it, and never a player |
| **Stays** | Within twelve blocks of where it was built |

**Gilt** is what a pulse costs: one gilt per pulse, however many it mends at once. A new golem has 16, and **gilt comes back by itself**, one every thirty seconds, all the way to full. Feed it to top it up faster:

| Item | Gives |
|---|---|
| Gold nugget | 1 gilt |
| Golden carrot | 8 gilt |

It holds at most 64. A pulse only goes out when someone in reach is actually hurt, so a golem in a quiet corner fills back up on its own. A long fight drains it faster than it refills - a pulse every five seconds against one gilt back every thirty - and that is when the gold in your pocket matters. Right-click one with an empty hand to see how much it has left. With none, it mends nobody and stops going to the hurt until some comes back.

**Its own body is repaired with gold ingots**, ten health each, the way an iron golem's is with iron. It has 40 health to an iron golem's 100: gold is the soft metal, and this is the golem you protect rather than the one that protects you. Golems mend each other, so two gold golems in reach of one another keep each other on their feet, at the cost of their gilt.

**Moving it.** Put a lead on it and walk it where you like. It settles wherever you take the lead off, and keeps to that spot from then on.

**Killed, it drops** three to five gold ingots.

## Piglins

A player within sixteen blocks of a gold golem counts, to a piglin, as a player wearing a piece of gold armour, and the piglin leaves them alone. That is all it changes:

- **Piglin brutes** ignore gold armour, and ignore the golem too.
- **Angering piglins still angers them.** Mining gold near them, opening a chest in their bastion, or hitting one turns them on you whether a golem is nearby or not.
- **Bartering** is untouched: throw them gold as you always would.

## Its Own Animal, On The Iron Golem's Bones

A gold golem is `gold-golem-justfatlard:gold_golem`, with its own goals, health, loot, name and body, and underneath it is the iron golem's own code.

**It is a golem of its own, between the two the game already has.** From the iron golem it takes the nose, the heavy brow over red eyes, and the arms that hang past its hips. From the copper golem it takes a wide head set forward, a body no taller than its legs are long, and something of its block on its head: where the copper golem wears a lightning rod, the gold golem wears a little bell, because a note block over gold plays the bell. It rings that bell note when it mends. No vines and no grain: it is polished smooth, the way gold is. It stands a little under two blocks tall, about two thirds of an iron golem.

It walks and swings its arms the way an iron golem does, because the iron golem's animation is what moves it. It does not crack as it is hurt: the iron golem's cracks are drawn for the iron golem's shape, and are kept off this one rather than landing on it in the wrong places. Its voice is the iron golem's, pitched up.

It is not an iron golem as far as a village is concerned: a village with a gold golem still makes iron golems of its own. Zombies and other monsters that pick fights with iron golems pick them with gold ones too.

A **Gold Golem Spawn Egg** sits with the other spawn eggs.

## Pandorical

Pandorical 15.21 or later is required on the server; the mod will not load without it.

**With Pandorical client-side**, a player sees the gold golem's own body.

**Without it**, a player sees an iron golem two thirds the size of the real ones, because the golem's size is the game's own scale attribute and every client reads that. It still mends them, still keeps piglins off them, and is still named Gold Golem wherever the game names it.

## Development

Installing, the art and the tests are in [DEVELOPMENT.md](DEVELOPMENT.md).

## License

MIT, see [LICENSE](LICENSE).
