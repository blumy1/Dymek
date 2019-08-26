package sb.blumek.dymek.observables;

import java.util.HashSet;
import java.util.Set;

public interface Observable {
    Set<Observer> observers = new HashSet<>();

    default void registerObserver(Observer observer) {
        observers.add(observer);
    }

    default void unregisterObserver(Observer observer) {
        observers.remove(observer);
    }

    default void notifyObservers() {
        for (Observer observer : observers) {
            observer.update(this);
        }
    }
}
