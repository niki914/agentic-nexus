#!/usr/bin/env python3
import json
import os
import sys
import urllib.error
import urllib.request


DEFAULT_REPO = "Xposed-Modules-Repo/com.niki914.nexus.agentic"


def api_headers() -> dict:
    headers = {
        "User-Agent": "release-new-version-skill",
        "Accept": "application/vnd.github+json",
    }
    token = os.getenv("GITHUB_TOKEN")
    if token:
        headers["Authorization"] = f"Bearer {token}"
    return headers


def fetch_by_api(repo: str) -> str:
    url = f"https://api.github.com/repos/{repo}/releases/latest"
    req = urllib.request.Request(url, headers=api_headers())
    with urllib.request.urlopen(req, timeout=20) as resp:
        payload = json.load(resp)
    tag = payload.get("tag_name")
    if not tag:
        raise RuntimeError("GitHub API response missing tag_name")
    return tag


def fetch_by_redirect(repo: str) -> str:
    url = f"https://github.com/{repo}/releases/latest"
    req = urllib.request.Request(url, headers={"User-Agent": "release-new-version-skill"})
    with urllib.request.urlopen(req, timeout=20) as resp:
        final_url = resp.geturl().rstrip("/")
    tag = final_url.split("/")[-1]
    if not tag or tag == "latest":
        raise RuntimeError(f"Unexpected redirect target: {final_url}")
    return tag


def main() -> int:
    repo = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_REPO
    errors = []

    for fetcher in (fetch_by_api, fetch_by_redirect):
        try:
            print(fetcher(repo))
            return 0
        except (urllib.error.HTTPError, urllib.error.URLError, RuntimeError, TimeoutError) as exc:
            errors.append(f"{fetcher.__name__}: {exc}")

    print("Failed to resolve latest release tag", file=sys.stderr)
    for item in errors:
        print(f"- {item}", file=sys.stderr)
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
