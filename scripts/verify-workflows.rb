#!/usr/bin/env ruby
# frozen_string_literal: true

require "yaml"

repository_root = File.expand_path("..", __dir__)
workflow_paths = Dir.glob(File.join(repository_root, ".github/workflows/*.yml"))
abort("No workflows found") if workflow_paths.empty?

workflow_paths.each do |path|
  parsed = YAML.safe_load(File.read(path), aliases: false)
  abort("Workflow must be a mapping: #{path}") unless parsed.is_a?(Hash)
  abort("Workflow has no jobs: #{path}") unless parsed["jobs"].is_a?(Hash)
end
