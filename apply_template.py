#!/usr/bin/env python3
import json
import os
import re
import sys
import shutil

def is_valid_package_name(name):
    if not name: return False
    return re.match(r'^[a-zA-Z_][a-zA-Z0-9_]*(\.[a-zA-Z_][a-zA-Z0-9_]*)+$', name) is not None

def main():
    if len(sys.argv) < 2:
        print("Usage: python3 apply_template.py <config.json>")
        sys.exit(1)
        
    config_file = sys.argv[1]
    if not os.path.exists(config_file):
        print(f"Error: {config_file} not found.")
        sys.exit(1)
        
    with open(config_file, 'r', encoding='utf-8') as f:
        try:
            config = json.load(f)
        except json.JSONDecodeError as e:
            print(f"Error parsing JSON: {e}")
            sys.exit(1)
            
    # Extract config
    app_id = config.get("applicationId")
    target_hook = config.get("targetHookPackage")
    proj_root = config.get("projectRootName")
    app_name = config.get("appName")
    xposed_desc = config.get("xposedDesc")
    base_pkg = config.get("basePackage")
    
    # Validation
    missing = []
    if not app_id: missing.append("applicationId")
    if not target_hook: missing.append("targetHookPackage")
    if not proj_root: missing.append("projectRootName")
    if not app_name: missing.append("appName")
    if not xposed_desc: missing.append("xposedDesc")
    if not base_pkg: missing.append("basePackage")
    
    if missing:
        print(f"Missing required fields in config.json: {', '.join(missing)}")
        sys.exit(1)
        
    if not is_valid_package_name(app_id):
        print(f"Invalid applicationId: {app_id}")
        sys.exit(1)
    if not is_valid_package_name(target_hook):
        print(f"Invalid targetHookPackage: {target_hook}")
        sys.exit(1)
    if not is_valid_package_name(base_pkg):
        print(f"Invalid basePackage: {base_pkg}")
        sys.exit(1)
    if not re.match(r'^[a-zA-Z0-9_-]+$', proj_root):
        print(f"Invalid projectRootName (only alphanumeric, _, - allowed): {proj_root}")
        sys.exit(1)
        
    print(f"Applying template with basePackage={base_pkg}...")

    # 1. Rename directories containing TEMPLATE_PKG
    src_dirs = []
    for root, dirs, files in os.walk('.', topdown=False):
        if any(ignored in root.split(os.sep) for ignored in ['.git', 'build', '.gradle', '.idea']):
            continue
        if root.endswith('TEMPLATE_PKG'):
            src_dirs.append(root)
            
    for src in src_dirs:
        parent = os.path.dirname(src)
        
        # Create new base_pkg directory structure
        new_base_path = os.path.join(parent, *base_pkg.split('.'))
        os.makedirs(new_base_path, exist_ok=True)
        
        # Move files from TEMPLATE_PKG to new_base_path
        for item in os.listdir(src):
            shutil.move(os.path.join(src, item), os.path.join(new_base_path, item))
            
        os.rmdir(src)
        print(f"Renamed directory: {src} -> {new_base_path}")

    # 2. File content replacements
    replacements = {
        '${APPLICATION_ID}': app_id,
        '${TARGET_HOOK_PACKAGE}': target_hook,
        '${PROJECT_ROOT_NAME}': proj_root,
        '${APP_NAME}': app_name,
        '${XPOSED_DESC}': xposed_desc,
        '${BASE_PACKAGE}': base_pkg
    }
    
    for root, dirs, files in os.walk('.'):
        if any(ignored in root.split(os.sep) for ignored in ['.git', 'build', '.gradle', '.idea']):
            continue
        for file in files:
            if file.endswith(('.kt', '.java', '.xml', '.kts', '.properties', '.pro')):
                filepath = os.path.join(root, file)
                try:
                    with open(filepath, 'r', encoding='utf-8') as f:
                        content = f.read()
                except UnicodeDecodeError:
                    continue
                    
                new_content = content
                for key, val in replacements.items():
                    new_content = new_content.replace(key, val)
                
                if new_content != content:
                    with open(filepath, 'w', encoding='utf-8') as f:
                        f.write(new_content)
                    print(f"Applied template to: {filepath}")

    print("Done! Project template instantiated successfully.")

if __name__ == '__main__':
    main()
