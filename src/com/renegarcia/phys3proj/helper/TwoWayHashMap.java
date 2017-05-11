/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.renegarcia.phys3proj.helper;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Rene
 */
public class TwoWayHashMap<K extends Object, V extends Object>
{
    private Map<K, V> forward = new HashMap<>();
    private Map<V, K> backward = new HashMap<>();

    
    public synchronized void put(K key, V value)
    {
        forward.put(key, value);
        backward.put(value, key);
    }

    public synchronized V getForward(K key)
    {
        return forward.get(key);
    }

    public synchronized K getBackward(V key)
    {
        return backward.get(key);
    }
}

