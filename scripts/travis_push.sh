#!/bin/sh

setup_git() {
  git config --global user.email "travis@travis-ci.org"
  git config --global user.name "Travis CI"
}

commit_website_files() {
  git add docs/public -f
  git commit --message "Documentation auto-generated by Travis build: $TRAVIS_BUILD_NUMBER [skip ci]"
}

upload_files() {
  git remote add documentation https://${GH_TOKEN}@github.com/kabirkbr/offernet.git
  git push documentation HEAD:master 
}

setup_git
commit_website_files
upload_files