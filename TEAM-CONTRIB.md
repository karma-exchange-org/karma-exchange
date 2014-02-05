# Instructions for members of the Karma Exchange product team to contribute changes

### Table of Contents

- [Uploading changes](#uploading)
- [Setup](#setup)
- [Helpful Git Links](#links)


<a name="uploading"/>
## Uploading changes

Our method for collobaration is based upon this wiki: https://gist.github.com/seshness/3943237

### Create a branch prior to making any changes

    $ git checkout -b <my-awesome-feature-branch-name>

### Make and test your changes

Make whatever changes you want to make. Make sure all tests pass and the UI features if any you modified work:

    $ mvn appengine:devserver

### Self review your changes

To see what files you've modified:

    $ git status

Use meld to take a look at your changes since your last commit (execute it from the root project directory):

    $ meld .

If you want to compare your changes including locally committed changes against the master branch use difftool:

    $ git difftool master

After you add or delete files make sure to run "git add ." (execute it from the root project directory).

    $ git add --all .
    $ git status

### Commit your changes

Commit your changes to your local branch:

    $ git commit -a

**Pro tip:** use the convenience script git-commit to do both git add and git-commit

    $ git-commit   

Then push your branch to the remote repository as a non-master branch:

    $ git push origin <my-awesome-feature-branch-name>

**Pro tip:** use the convenience script git-push instead (saves typing)

    $ git-push


### Submit a pull request for your branch

* Go to the [karma exchange project](https://github.com/karma-exchange-org/karma-exchange.git) on github.
* Click "Branches"
* Select your branch
* Click "Pull Request" to submit a pull request.

### Reviewing the code

The code should ideally be reviewed by someone else in the organization. See the [git hub pull tutorial](https://help.github.com/articles/using-pull-requests#managing-pull-requests) to see how pull requests are processed by the code reviewer.

To incorporate code review feedback just follow the prior modification and push instructions and re-run your tests, but skip the step of creating a new branch:

    <make your changes>
    $ mvn appengine:devserver
    $ git add --all .
    $ git commit -a    
    $ git push origin <my-awesome-feature-branch-name>

### Merging the code

Once you have received code review approval follow the [merging directly on github instructions](https://help.github.com/articles/merging-a-pull-request). In most cases it should just be as simple as clicking the "Merge pull request" button.

After the code is merged, delete your branch on git hub by clicking "delete branch".

### Cleanup your local branch

Sync your master branch to the latest changes:

    $ git checkout master
    $ git remote update --prune
    $ git pull

Delete your merged branch:

    $ git branch -d <my-awesome-feature-branch-name>

**Pro tip:** use the convenience script git-cleanup to do all four commands above

    $ git-cleanup

<a name="setup"/>
## Setup

### Setting up git on your computer

Read https://help.github.com/articles/set-up-git

    git config --global user.name "First Last"
    git config --global user.email "fake.email@gmail.com"
    git config --global core.editor $EDITOR
    git config --global credential.helper cache
    git config --global credential.helper 'cache --timeout=360000'

Note: make sure to register your fake email in github.

### Install meld

Meld is my preferred tool for diffing changes. It's available at http://meldmerge.org/

After you install meld update your gitconfig to use meld as the default diff tool:

    git config --global diff.tool meld
    git config --global diff.guitool meld
    git config --global difftool.prompt false

### Getting a local copy of the repository

Assuming that you're placing the repository in ~/src

    mkdir ~/src
    cd ~/src
    git clone https://github.com/karma-exchange-org/karma-exchange.git
    cd karma-exchange
    git config --list

If you have configured things as suggested above you should see output like the following from git config:

    $ git config --list
    user.name=Amir Valiani
    user.email=first.last@gmail.com
    credential.helper=cache --timeout=360000
    core.editor=emacs
    diff.tool=meld
    diff.guitool=meld
    difftool.prompt=false
    core.repositoryformatversion=0
    core.filemode=true
    core.bare=false
    core.logallrefupdates=true
    remote.origin.fetch=+refs/heads/*:refs/remotes/origin/*
    remote.origin.url=https://github.com/karma-exchange-org/karma-exchange.git
    branch.master.remote=origin
    branch.master.merge=refs/heads/master

### Convenience scripts

I've created a few convenience scripts that I recommend you download [from here](https://www.dropbox.com/sh/qbeli6omtrbdoyu/oG4QNpe79L)

    git-commit.sh
    git-push.sh
    git-cleanup.sh

Once you download them put them in your preferred directory (example below uses ~/bin) and create aliases to them:

    $ cat ~/.bash_aliases
    alias git-cleanup=~/bin/git-cleanup.sh
    alias git-commit=~/bin/git-commit.sh
    alias git-push=~/bin/git-push.sh

<a name="links"/>
## Helpful Git Links

1. Github:
   * https://help.github.com/articles/set-up-git
2. Github collaboration. This is our template for collaboration:
   * https://gist.github.com/seshness/3943237
3. Git:
   * http://git-scm.com/documentation
   * http://www.vogella.com/articles/Git/article.html
   * http://gitref.org/basic/
