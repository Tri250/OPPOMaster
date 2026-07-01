//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

namespace alcedo::sidecar_client {

template <typename Derived, typename Proto>
struct ProtoDto {
  using ProtoType = Proto;

  [[nodiscard]] static auto FromProto(const Proto& proto) -> Derived {
    return Derived::FromProto(proto);
  }

  void ToProto(Proto* proto) const { static_cast<const Derived*>(this)->ToProto(proto); }
};

}  // namespace alcedo::sidecar_client
