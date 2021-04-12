package com.jaysmito.jmbasic.commons;

public class KeyValue<K, V>{
    public K key;
    public V value;

    public KeyValue(K key, V value){
        this.key = key;
        this.value = value;
    }

    @Override
    public String toString() {
        return key.toString() + ":" + value.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return this.key.equals(((KeyValue)obj).key);
    }
}
