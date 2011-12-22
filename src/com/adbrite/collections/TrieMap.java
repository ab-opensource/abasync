package com.adbrite.collections;

import java.util.ArrayList;
import java.util.Collection;

// 20110202 JRG: packaged
/**
 * Implements very fast dictionary storage and retrieval.
 * Only depends upon the core String class.
 * 
 * @author Melinda Green - (c) 2010 Superliminal Software.
 * Free for all uses with attribution.
 * @author Daniel Issen - changed naming style, implemented floor()
 */

//TODO: extract bytes interface and optimize by eliminating key.substring(1)
public class TrieMap<V> {
  /*
   * Implementation of a trie tree. (see http://en.wikipedia.org/wiki/Trie)
   * though I made it faster and more compact for long key strings by
   * building tree nodes only as needed to resolve collisions.  Each letter
   * of a key is the index into the following array.  Values stored in the
   * array are either a Leaf containing the user's value or another TrieMap
   * node if more than one key shares the key prefix up to that point.  Null
   * elements indicate unused, I.E. available slots.
   */
  private Object[] _children = new Object[256];
  private Object _prefix_value; // Used only for values of prefix keys.
    
  // Simple container for a string-value pair.
  private static class Leaf {
    public String _string;
    public Object _value;
    public Leaf(String str, Object val) {
      _string = str;
      _value = val;
    }
  }
    
  public TrieMap() {
  }

  public boolean isEmpty() {
    if(_prefix_value != null) {
      return false;
    }
    for(Object o : _children) {
      if(o != null) {
        return false;
      }
    }
    return true;
  }
    
    
  /**
   * Inserts a key/value pair.
   * 
   * @param key may be empty or contain low-order chars 0..255 but must not
   * be null.
   * @param val Your data. Any data class except another TrieMap. Null values
   * erase entries.
   */
  public void put(String key, Object val) {
    if(key.length() == 0) {
      // All of the original key's chars have been nibbled away 
      // which means this node will store this key as a prefix of other keys.
      _prefix_value = val; // Note: possibly removes or updates an item.
      return;
    }
    char c = key.charAt(0);
    Object child = _children[c];
    if(child == null) { // Unused slot means no collision so just store and
                        // return;
      if(val == null) {
        return; // Don't create a leaf to store a null value.
      }
      _children[c] = new Leaf(key, val);
      return;
    }
    if(child.getClass() == TrieMap.class) {
      // Collided with an existing sub-branch so nibble a char and recurse.
      TrieMap childTrie = (TrieMap)child;
      childTrie.put(key.substring(1), val);
      if(val == null && childTrie.isEmpty()) {
        _children[c] = null; // put() must have erased final entry so prune
                             // branch.
      }
      return;
    }
    // Collided with a leaf 
    if(val == null) {
      _children[c] = null; // Null value means to remove any previously
                           // stored value.
      return;
    }
    // Sprout a new branch to hold the colliding items.
    Leaf leaf = (Leaf)child;
    TrieMap branch = new TrieMap();
    branch.put(key.substring(1), val); // Store new value in new subtree.
    branch.put(leaf._string.substring(1), leaf._value); // Plus the one we
                                                        // collided with.
    _children[c] = branch;
  }
  
  public void remove(String key) {
	  put(key, null);
  }


  /**
   * Retrieve a value for a given key or null if not found.
   */

  public V get(String key) {
    if(key.length() == 0) {
      // All of the original key's chars have been nibbled away 
      // which means this key is a prefix of another.
      return (V)_prefix_value;
    }
    char c = key.charAt(0);
    Object child = _children[c];
    if(child == null) {
      return null; // Not found.
    }
    if(child.getClass() == TrieMap.class) { // Hash collision. Nibble first
					    // char, and recurse.
      return (V)((TrieMap)child).get(key.substring(1));
    }
    if(child.getClass() == Leaf.class) {
      // child contains a user datum, but does the key match its substring?
      Leaf leaf = (Leaf)child;
      if(key.equals(leaf._string)) {
        return (V)leaf._value; // Return user's data value.
      }
    }
    return null; // Not found.
  }

  /**
   * returns the object that matches the maximal prefix of the key
   */
  public V floor(String key) {
    TrieMap current = this;
    Object ret = null;
    int index = 0;
    for(index = 0; index < key.length(); ++index ) {
      char c = key.charAt(index);
      Object child = current._children[c];
      if(null == child) {
        return (V)ret;
      }
      if(child.getClass() == Leaf.class) {
        Leaf leaf = (Leaf)child;
        if(key.startsWith(leaf._string,index)) {
          return (V)leaf._value;
        } else {
          return (V)ret;
        }
      } else {
        current = (TrieMap)child;
        ret = current._prefix_value;
      }
    }
    return (V)ret;
  }
    
}
