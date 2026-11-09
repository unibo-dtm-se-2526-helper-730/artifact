// semantic-release configuration, run by .github/workflows/deploy.yml.
//
// The next version comes from the Conventional Commits merged since the last tag:
//   fix: / perf:                      -> patch  (0.2.0 -> 0.2.1)
//   feat:                             -> minor  (0.2.0 -> 0.3.0)
//   feat!: or "BREAKING CHANGE:"      -> major  (0.2.0 -> 1.0.0)
//   docs: ci: build: chore: test: ... -> no release
//
// A real release happens only on main. On any other branch (release/*, feature/*)
// the deploy workflow sets RELEASE_DRY_RUN=true: the run only prints the version
// that branch would produce once merged, and publishes nothing.

const dryRun = process.env.RELEASE_DRY_RUN === 'true';
const branch = process.env.GITHUB_REF_NAME;

module.exports = {
  branches: dryRun && branch ? [branch] : ['main'],
  dryRun,
  tagFormat: 'v${version}',
  plugins: [
    ['@semantic-release/commit-analyzer', { preset: 'conventionalcommits' }],
    ['@semantic-release/release-notes-generator', { preset: 'conventionalcommits' }],
    // Runs before the tag is created: if an image fails to build or push, nothing is released
    [
      '@semantic-release/exec',
      { prepareCmd: 'bash .github/scripts/publish-images.sh ${nextRelease.version}' },
    ],
    // GitHub Release with the generated notes; no issue is opened when a release fails
    ['@semantic-release/github', { failComment: false }],
  ],
};
