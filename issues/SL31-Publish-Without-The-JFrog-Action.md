# SL31 — Publish without the JFrog action

**Status:** Idea.

**What must be true.** The Build's Publish job reaches Artifactory with `curl` and the token in its environment, as
the agent repositories' Publish does, instead of through `jfrog/setup-jfrog-cli`, so one third-party action less
handles the publishing token.
