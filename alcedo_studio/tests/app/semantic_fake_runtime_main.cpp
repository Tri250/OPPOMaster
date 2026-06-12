//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include <chrono>
#include <csignal>
#include <filesystem>
#include <fstream>
#include <iostream>
#include <string>
#include <thread>
#include <vector>

namespace {

volatile std::sig_atomic_t g_should_exit = 0;

void SignalHandler(int signal) {
  (void)signal;
  g_should_exit = 1;
}

auto ArgValue(const std::vector<std::string>& args, const std::string& name,
              const std::string& fallback = {}) -> std::string {
  for (size_t index = 0; index + 1 < args.size(); ++index) {
    if (args[index] == name) {
      return args[index + 1];
    }
  }
  return fallback;
}

auto HasArg(const std::vector<std::string>& args, const std::string& name) -> bool {
  for (const auto& arg : args) {
    if (arg == name) {
      return true;
    }
  }
  return false;
}

}  // namespace

int main(int argc, char** argv) {
  std::vector<std::string> args;
  args.reserve(static_cast<size_t>(argc));
  for (int index = 1; index < argc; ++index) {
    args.emplace_back(argv[index]);
  }

  const auto record_path = ArgValue(args, "--record-args");
  if (!record_path.empty()) {
    std::ofstream out(record_path, std::ios::binary);
    for (const auto& arg : args) {
      out << arg << '\n';
    }
  }

  if (HasArg(args, "--exit-now")) {
    return std::stoi(ArgValue(args, "--exit-code", "0"));
  }

  if (HasArg(args, "--ignore-terminate")) {
    std::signal(SIGTERM, SIG_IGN);
    std::signal(SIGINT, SIG_IGN);
  } else {
    std::signal(SIGTERM, SignalHandler);
    std::signal(SIGINT, SignalHandler);
  }

  std::cout << "semantic fake runtime started" << std::endl;

  const auto exit_after_ms = std::stoi(ArgValue(args, "--exit-after-ms", "0"));
  const auto sleep_ms = std::stoi(ArgValue(args, "--sleep-ms", "30000"));
  const auto start = std::chrono::steady_clock::now();
  while (!g_should_exit) {
    std::this_thread::sleep_for(std::chrono::milliseconds(10));
    const auto elapsed =
        std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::steady_clock::now() -
                                                              start)
            .count();
    if (exit_after_ms > 0 && elapsed >= exit_after_ms) {
      std::cerr << "semantic fake runtime self-exit" << std::endl;
      return std::stoi(ArgValue(args, "--exit-code", "0"));
    }
    if (sleep_ms > 0 && elapsed >= sleep_ms) {
      break;
    }
  }

  return 0;
}
