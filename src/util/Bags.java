package util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import net.automatalib.automata.transducers.MealyMachine;
import net.automatalib.commons.util.Pair;
import net.automatalib.util.automata.equivalence.DeterministicEquivalenceTest;
import net.automatalib.words.Alphabet;

public class Bags {
    Alphabet<String> alphabet;
    Integer mostFrequentSize = -1;
    Integer secondMostFrequentSize = -1;
    Integer mostFrequentIndex = -1;
    Integer secondMostFrequentIndex = -1;
    Integer highestCount = -1;

    Integer currentSize = -1;
    Integer currentIndex = -1;
    HashMap<Integer, List<Pair<Integer, MealyMachine<?, String, ?, String>>>> bags = new HashMap<>();

    public Bags(Alphabet<String> inputAlphabet) {
        this.alphabet = inputAlphabet;
    }

    public void addHypothesis(Integer lifetime, MealyMachine<?, String, ?, String> hyp) {

        if (hyp == null && currentSize == -1) {
            // We got notified of a new lifetime but we are empty!
            return;
        }

        if (hyp == null) {
            Integer newCount = bags.get(currentSize).get(currentIndex).getFirst() + lifetime;
            MealyMachine<?, String, ?, String> newHyp = bags.get(currentSize).get(currentIndex).getSecond();
            bags.get(currentSize).set(currentIndex, Pair.of(newCount, newHyp));

            if (newCount > highestCount) {
                highestCount = newCount;

                secondMostFrequentIndex = mostFrequentIndex;
                secondMostFrequentSize = mostFrequentSize;

                mostFrequentIndex = currentIndex;
                mostFrequentSize = currentSize;
            }

            return;
        }

        Boolean addedToBag = false;
        List<Pair<Integer, MealyMachine<?, String, ?, String>>> innerBags = bags.computeIfAbsent(hyp.size(),
                k -> new LinkedList<>());
        for (int bagIndex = 0; bagIndex < innerBags.size(); bagIndex++) {
            Boolean equivalent = DeterministicEquivalenceTest.findSeparatingWord(hyp,
                    innerBags.get(bagIndex).getSecond(), alphabet) == null;
            if (equivalent) {
                MealyMachine<?, String, ?, String> newMealy = innerBags.get(bagIndex).getSecond();
                Integer newQueryCount = innerBags.get(bagIndex).getFirst() + lifetime;
                if (newQueryCount > highestCount) {
                    highestCount = newQueryCount;

                    secondMostFrequentIndex = mostFrequentIndex;
                    secondMostFrequentSize = mostFrequentSize;

                    mostFrequentIndex = bagIndex;
                    mostFrequentSize = hyp.size();
                }
                if (hyp.size() < newMealy.size()) {
                    newMealy = hyp;
                }
                innerBags.set(bagIndex, Pair.of(newQueryCount, newMealy));

                addedToBag = true;
                currentSize = hyp.size();
                currentIndex = bagIndex;
                break;
            }

        }

        if (!addedToBag) {
            innerBags.add(Pair.of(lifetime, hyp));
            currentSize = hyp.size();
            currentIndex = innerBags.size() - 1;
            if (lifetime > highestCount) {
                highestCount = lifetime;

                secondMostFrequentIndex = mostFrequentIndex;
                secondMostFrequentSize = mostFrequentSize;

                mostFrequentIndex = innerBags.size() - 1;
                mostFrequentSize = hyp.size();
            }
        }

        // FIXME: Testing! remove when sure.
        assert DeterministicEquivalenceTest.findSeparatingWord(hyp,
                bags.get(currentSize).get(currentIndex).getSecond(), alphabet) == null;
    }

    public MealyMachine<?, String, ?, String> getMostFrequent() {
        if (currentIndex == -1) {
            return null;
        }

        return bags.get(mostFrequentSize).get(mostFrequentIndex).getSecond();
    }

    public Integer getLead() {
        int secondCount = (mostFrequentIndex == secondMostFrequentIndex && mostFrequentSize == secondMostFrequentSize)
                ? 0
                : bags.get(secondMostFrequentSize).get(secondMostFrequentIndex).getFirst();
        return bags.get(mostFrequentSize).get(mostFrequentIndex).getFirst() - secondCount;
    }

}
