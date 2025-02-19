// Copyright (c) YugaByte, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distributed under the License
// is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
// or implied.  See the License for the specific language governing permissions and limitations
// under the License.
//

#ifndef YB_DOCDB_VALUE_TYPE_H_
#define YB_DOCDB_VALUE_TYPE_H_

#include <bitset>
#include <string>

#include <boost/preprocessor/seq/for_each.hpp>

#include "yb/util/enums.h"
#include "yb/util/math_util.h"
#include "yb/util/slice.h"

namespace yb {
namespace docdb {

#define DOCDB_VALUE_TYPES \
    /* This ValueType is used as -infinity for scanning purposes only. */\
    ((kLowest, 0)) \
    /* Prefix for transaction apply state records. */ \
    ((kTransactionApplyState, 7)) \
    /* Externally received transaction id */ \
    ((kExternalTransactionId, 8)) \
    /* Obsolete intent prefix. Should be deleted when DBs in old format are gone. */ \
    ((kObsoleteIntentPrefix, 10)) \
    /* We use ASCII code 13 in order to have it before all other value types which can occur in */ \
    /* key, so intents will be written in the same order as original keys for which intents are */ \
    /* written. */ \
    ((kIntentTypeSet, 13)) \
    /* Obsolete intent prefix. Should be deleted when DBs in old format are gone. */ \
    /* It has different values to intent type set entries, that was not optimised for RocksDB */ \
    /* lookup. */ \
    /* I.e. strong/weak and read/write bits are swapped. */ \
    ((kObsoleteIntentTypeSet, 15)) \
    /* Obsolete intent prefix. Should be deleted when DBs in old format are gone. */ \
    /* Old intent type had 6 different types of intents, one for each possible intent. */ \
    /* Intent type set has 4 different types of intents, but allow their combinations. */ \
    ((kObsoleteIntentType, 20)) \
    /* Value type that is greater than any of intent types. */ \
    /* It is NOT stored in DB, so should be updated if we add new intent value type */ \
    ((kGreaterThanIntentType, 21)) \
    /* This indicates the end of the "hashed" or "range" group of components of the primary */ \
    /* key. This needs to sort before all other value types, so that a DocKey that has a prefix */ \
    /* of the sequence of components of another key sorts before the other key. kGroupEnd is */ \
    /* also used as the end marker for a frozen value. */ \
    ((kGroupEnd, '!')) /* ASCII code 33 -- we pick the lowest code graphic character. */ \
    /* HybridTime must be lower than all other primitive types (other than kGroupEnd) so that */ \
    /* SubDocKeys that have fewer subkeys within a document sort above those that have all the */ \
    /* same subkeys and more. In our MVCC document model layout the hybrid time always appears */ \
    /* at the end of the key. */ \
    ((kHybridTime, '#')) /* ASCII code 35 (34 is double quote, which would be a bit confusing */ \
                         /* here). */ \
    /* Primitive value types */ \
    \
    /* Null must be lower than the other primitive types so that it compares as smaller than */ \
    /* them. It is used for frozen CQL user-defined types (which can contain null elements) on */ \
    /* ASC columns. */ \
    ((kNullLow, '$')) /* ASCII code 36 */ \
    /* Counter to check cardinality. */ \
    ((kCounter, '%')) /* ASCII code 37 */ \
    /* Forward and reverse mappings for sorted sets. */ \
    ((kSSForward, '&')) /* ASCII code 38 */ \
    ((kSSReverse, '\'')) /* ASCII code 39 */ \
    ((kRedisSet, '(')) /* ASCII code 40 */ \
    ((kRedisList, ')')) /* ASCII code 41*/ \
    /* This is the redis timeseries type. */ \
    ((kRedisTS, '+')) /* ASCII code 43 */ \
    ((kRedisSortedSet, ',')) /* ASCII code 44 */ \
    ((kInetaddress, '-'))  /* ASCII code 45 */ \
    ((kInetaddressDescending, '.'))  /* ASCII code 46 */ \
    ((kPgTableOid, '0')) /* ASCII code 48 */ \
    ((kJsonb, '2')) /* ASCII code 50 */ \
    ((kFrozen, '<')) /* ASCII code 60 */ \
    ((kFrozenDescending, '>')) /* ASCII code 62 */ \
    ((kArray, 'A'))  /* ASCII code 65 */ \
    ((kVarInt, 'B')) /* ASCII code 66 */ \
    ((kFloat, 'C'))  /* ASCII code 67 */ \
    ((kDouble, 'D'))  /* ASCII code 68 */ \
    ((kDecimal, 'E'))  /* ASCII code 69 */ \
    ((kFalse, 'F'))  /* ASCII code 70 */ \
    ((kUInt16Hash, 'G'))  /* ASCII code 71 */ \
    ((kInt32, 'H'))  /* ASCII code 72 */ \
    ((kInt64, 'I'))  /* ASCII code 73 */ \
    ((kSystemColumnId, 'J'))  /* ASCII code 74 */ \
    ((kColumnId, 'K'))  /* ASCII code 75 */ \
    ((kDoubleDescending, 'L'))  /* ASCII code 76 */ \
    ((kFloatDescending, 'M')) /* ASCII code 77 */ \
    ((kUInt32, 'O'))  /* ASCII code 79 */ \
    ((kString, 'S'))  /* ASCII code 83 */ \
    ((kTrue, 'T'))  /* ASCII code 84 */ \
    ((kUInt64, 'U')) /* ASCII code 85 */ \
    ((kTombstone, 'X'))  /* ASCII code 88 */ \
    ((kExternalIntents, 'Z')) /* ASCII code 90 */ \
    ((kArrayIndex, '['))  /* ASCII code 91 */ \
    ((kCollString, '\\'))  /* ASCII code 92 */ \
    ((kCollStringDescending, ']'))  /* ASCII code 93 */ \
    \
    /* We allow putting a 32-bit hash in front of the document key. This hash is computed based */ \
    /* on the "hashed" components of the document key that precede "range" components. */ \
    \
    ((kUuid, '_')) /* ASCII code 95 */ \
    ((kUuidDescending, '`')) /* ASCII code 96 */ \
    ((kStringDescending, 'a'))  /* ASCII code 97 */ \
    ((kInt64Descending, 'b'))  /* ASCII code 98 */ \
    ((kTimestampDescending, 'c'))  /* ASCII code 99 */ \
    ((kDecimalDescending, 'd'))  /* ASCII code 100 */ \
    ((kInt32Descending, 'e'))  /* ASCII code 101 */ \
    ((kVarIntDescending, 'f'))  /* ASCII code 102 */ \
    ((kUInt32Descending, 'g'))  /* ASCII code 103 */ \
    ((kTrueDescending, 'h'))  /* ASCII code 104 */ \
    ((kFalseDescending, 'i'))  /* ASCII code 105 */ \
    ((kUInt64Descending, 'j')) /* ASCII code 106 */ \
    \
    /* Flag type for merge record flags */ \
    ((kMergeFlags, 'k')) /* ASCII code 107 */ \
    /* Indicator for whether an intent is for a row lock. */ \
    ((kRowLock, 'l'))  /* ASCII code 108 */ \
    ((kBitSet, 'm')) /* ASCII code 109 */ \
    ((kSubTransactionId, 'n')) /* ASCII code 110 */ \
    /* Timestamp value in microseconds */ \
    ((kTimestamp, 's'))  /* ASCII code 115 */ \
    /* TTL value in milliseconds, optionally present at the start of a value. */ \
    ((kTtl, 't'))  /* ASCII code 116 */ \
    ((kUserTimestamp, 'u'))  /* ASCII code 117 */ \
    ((kGinNull, 'v')) /* ASCII code 118 */ \
    ((kWriteId, 'w')) /* ASCII code 119 */ \
    ((kTransactionId, 'x')) /* ASCII code 120 */ \
    ((kTableId, 'y')) /* ASCII code 121 */ \
    \
    ((kObject, '{'))  /* ASCII code 123 */ \
    \
    /* Null desc must be higher than the other descending primitive types so that it compares */ \
    /* as bigger than them. It is used for frozen CQL user-defined types (which can contain */ \
    /* null elements) on DESC columns. */ \
    ((kNullHigh, '|')) /* ASCII code 124 */ \
    \
    /* This is only needed when used as the end marker for a frozen value on a DESC column. */ \
    ((kGroupEndDescending, '}')) /* ASCII code 125 -- we pick the highest value below kHighest. */ \
    \
    /* This ValueType is used as +infinity for scanning purposes only. */ \
    ((kHighest, '~')) /* ASCII code 126 */ \
    \
    /* This is used for sanity checking. */ \
    ((kInvalid, 127)) \
    \
    /* ValueType which lexicographically higher than any other byte and is not used for */ \
    /* encoding value type. */ \
    ((kMaxByte, '\xff'))

YB_DEFINE_ENUM(ValueType, DOCDB_VALUE_TYPES);

#define DOCDB_VALUE_TYPE_AS_CHAR_IMPL(name, value) static constexpr char name = value;
#define DOCDB_VALUE_TYPE_AS_CHAR(i, data, entry) DOCDB_VALUE_TYPE_AS_CHAR_IMPL entry

struct ValueTypeAsChar {
  BOOST_PP_SEQ_FOR_EACH(DOCDB_VALUE_TYPE_AS_CHAR, ~, DOCDB_VALUE_TYPES)
};

// All primitive value types fall into this range, but not all value types in this range are
// primitive (e.g. object and tombstone are not).

constexpr ValueType kMinPrimitiveValueType = ValueType::kNullLow;
constexpr ValueType kMaxPrimitiveValueType = ValueType::kNullHigh;

// kArray is handled slightly differently and hence we only have
// kObject, kRedisTS, kRedisSet, and kRedisList.
constexpr inline bool IsObjectType(const ValueType value_type) {
  return value_type == ValueType::kRedisTS || value_type == ValueType::kObject ||
      value_type == ValueType::kRedisSet || value_type == ValueType::kRedisSortedSet ||
      value_type == ValueType::kSSForward || value_type == ValueType::kSSReverse ||
      value_type == ValueType::kRedisList;
}

constexpr inline bool IsCollectionType(const ValueType value_type) {
  return IsObjectType(value_type) || value_type == ValueType::kArray;
}

constexpr inline bool IsRegulaDBInternalRecordKeyType(const ValueType value_type) {
  // For regular db:
  // - transaction apply state records.
  return value_type == ValueType::kTransactionApplyState;
}

constexpr inline bool IsIntentsDBInternalRecordKeyType(const ValueType value_type) {
  // For intents db:
  // - reverse index from transaction id to keys of write intents belonging to that transaction.
  // - external transaction records (transactions that originated on a CDC producer).
  return value_type == ValueType::kExternalTransactionId ||
         value_type == ValueType::kTransactionId;
}

constexpr inline bool IsInternalRecordKeyType(const ValueType value_type) {
  return IsRegulaDBInternalRecordKeyType(value_type) ||
         IsIntentsDBInternalRecordKeyType(value_type);
}

constexpr inline bool IsPrimitiveValueType(const ValueType value_type) {
  return (kMinPrimitiveValueType <= value_type && value_type <= kMaxPrimitiveValueType &&
          !IsCollectionType(value_type) &&
          value_type != ValueType::kTombstone) ||
          value_type == ValueType::kTransactionApplyState ||
          value_type == ValueType::kExternalTransactionId;
}

constexpr inline bool IsSpecialValueType(ValueType value_type) {
  return value_type == ValueType::kLowest || value_type == ValueType::kHighest ||
         value_type == ValueType::kMaxByte || value_type == ValueType::kIntentTypeSet ||
         value_type == ValueType::kGreaterThanIntentType;
}

constexpr inline bool IsPrimitiveOrSpecialValueType(ValueType value_type) {
  return IsPrimitiveValueType(value_type) || IsSpecialValueType(value_type);
}

// Decode the first byte of the given slice as a ValueType.
inline ValueType DecodeValueType(const rocksdb::Slice& value) {
  return value.empty() ? ValueType::kInvalid : static_cast<ValueType>(value.data()[0]);
}

// Decode the first byte of the given slice as a ValueType and consume it.
inline ValueType ConsumeValueType(rocksdb::Slice* slice) {
  return slice->empty() ? ValueType::kInvalid
                        : static_cast<ValueType>(slice->consume_byte());
}

constexpr inline ValueType DecodeValueType(char value_type_byte) {
  return static_cast<ValueType>(value_type_byte);
}

// Checks if a value is a merge record, meaning it begins with the
// kMergeFlags value type. Currently, the only merge records supported are
// TTL records, when the flags value is 0x1. In the future, value
// merge records may be implemented, such as a +1 merge record for INCR.
inline bool IsMergeRecord(const rocksdb::Slice& value) {
  return DecodeValueType(value) == ValueType::kMergeFlags;
}

}  // namespace docdb
}  // namespace yb

#endif  // YB_DOCDB_VALUE_TYPE_H_
