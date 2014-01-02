/**
 * JavaScript BitArray - v0.2.0
 *
 * Licensed under the revised BSD License.
 * Copyright 2010-2012 Bram Stein
 * All rights reserved.
 */

/**
 * Creates a new empty BitArray with the given length.
 */
var BitArray = function (size) {
    this.values = [];
    this._length = size;

    for (var i = 0; i < Math.ceil(size / 32); i += 1) {
        this.values[i] = 0;
    }
};

/**
 * Returns the total number of bits in this BitArray.
 */
BitArray.prototype.size = function () {
    return this.values.length * 32;
};

/**
 * Returns the number of settable bits in this BitArray.
 */
Object.defineProperty(BitArray.prototype, "length", {
    get: function() {
        return this._length;
    }
});

/**
 * Sets the bit at index to a value (boolean.)
 */
BitArray.prototype.set = function (index, value) {
    var i = Math.floor(index / 32);
    // Since "undefined | 1 << index" is equivalent to "0 | 1 << index" we do not need to initialise the array explicitly here.
    if (value) {
        this.values[i] |= 1 << index - i * 32;
    } else {
        this.values[i] &= ~(1 << index - i * 32);
    }
    return this;
};

/**
 * Toggles the bit at index. If the bit is on, it is turned off. Likewise, if the bit is off it is turned on.
 */
BitArray.prototype.toggle = function (index) {
    var i = Math.floor(index / 32);
    this.values[i] ^= 1 << index - i * 32;
    return this;
};

/**
 * Returns the value of the bit at index (boolean.)
 */
BitArray.prototype.get = function (index) {
    var i = Math.floor(index / 32);
    return !!(this.values[i] & (1 << index - i * 32));
};

/**
 * Resets the BitArray so that it is empty and can be re-used.
 */
BitArray.prototype.reset = function () {
    this.values = [];
    return this;
};

/**
 * Returns a copy of this BitArray.
 */
BitArray.prototype.copy = function () {
    var cp = new BitArray();
    cp.length = this.length;
    cp.values = [].concat(this.values);
    return cp;
};

/**
 * Returns true if this BitArray equals another. Two BitArrays are considered
 * equal if both have the same length and bit pattern.
 */
BitArray.prototype.equals = function (x) {
    return this.values.length === x.values.length &&
        this.values.every(function (value, index) {
        return value === x.values[index];
    });
};

/**
 * Returns the JSON representation of this BitArray.
 */
BitArray.prototype.toJSON = function () {
    return JSON.stringify(this.values);
};

/**
 * Returns a string representation of the BitArray with bits
 * in logical order.
 */
BitArray.prototype.toString = function () {
    return this.toArray().map(function (value) {
        return value ? '1' : '0';
    }).join('');
};

/**
 * Returns a string representation of the BitArray with bits
 * in mathemetical order.
 */
BitArray.prototype.toBinaryString = function () {
    return this.toArray().map(function (value) {
        return value ? '1' : '0';
    }).reverse().join('');
};

/**
 * Returns a hexadecimal string representation of the BitArray
 * with bits in logical order.
 */
BitArray.prototype.toHexString = function () {
  var result = [];

  for (var i = 0; i < this.values.length; i += 1) {
    result.push(('00000000' + (this.values[i] >>> 0).toString(16)).slice(-8));
  }
  return result.join('');
};

/**
 * Convert the BitArray to an Array of boolean values.
 */
BitArray.prototype.toArray = function () {
    var result = [];

    for (var i = 0; i < this.values.length * 32; i += 1) {
        result.push(this.get(i));
    }
    return result;
};

/**
 * Returns the total number of bits set to one in this BitArray.
 */
BitArray.prototype.count = function () {
    var total = 0;

    // If we remove the toggle method we could efficiently cache the number of bits without calculating it on the fly.
    this.values.forEach(function (x) {
        // See: http://bits.stephan-brumme.com/countBits.html for an explanation
        x  = x - ((x >> 1) & 0x55555555);
        x  = (x & 0x33333333) + ((x >> 2) & 0x33333333);
        x  = x + (x >> 4);
        x &= 0xF0F0F0F;

        total += (x * 0x01010101) >> 24;
    });
    return total;
};

/**
 * Inverts this BitArray.
 */
BitArray.prototype.not = function () {
    this.values = this.values.map(function (v) {
        return ~v;
    });
    return this;
};

/**
 * Bitwise OR on the values of this BitArray using BitArray x.
 */
BitArray.prototype.or = function (x) {
    if (this.values.length !== x.values.length) {
        throw 'Arguments must be of the same length.';
    }
    this.values = this.values.map(function (v, i) {
        return v | x.values[i];
    });
    return this;
};

/**
 * Bitwise AND on the values of this BitArray using BitArray x.
 */
BitArray.prototype.and = function (x) {
    if (this.values.length !== x.values.length) {
        throw 'Arguments must be of the same length.';
    }
    this.values = this.values.map(function (v, i) {
        return v & x.values[i];
    });
    return this;
};

/**
 * Bitwise XOR on the values of this BitArray using BitArray x.
 */
BitArray.prototype.xor = function (x) {
    if (this.values.length !== x.values.length) {
        throw 'Arguments must be of the same length.';
    }
    this.values = this.values.map(function (v, i) {
        return v ^ x.values[i];
    });
    return this;
};

// module.exports = BitArray;
