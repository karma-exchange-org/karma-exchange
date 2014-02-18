# Instructions for members of the Karma Exchange product team to contribute changes

### Table of Contents

- [Uploading changes](#uploading)
- [Setup](#setup)
- [Helpful Git Links](#links)


<a name="uploading"/>
## Uploading changes

Our method for collobaration is based upon this wiki: https://gist.github.com/seshness/3943237

### Create a branch prior to making any changes

Use a temporary branch name until you are ready to do a push.

    $ git checkout -b <temp-branch-name>

### Make and test your changes

Make whatever changes you want to make. Make sure all tests pass and the UI features if any you modified work:

    $ mvn appengine:devserver

### Self review your changes

To see how your branch differs from the master branch execute the alias git-diff (see setup).

    $ git-diff

If you want to just take a look at your changes since your last commit use meld directly (execute it from the root project directory):

    $ meld .

To see what files you've modified since the last commit:

    $ git status

After you add or delete files make sure to run the following commands (execute them from the root project directory).

    $ git add --all .
    $ git status

### Commit your changes

Commit your changes to your local branch:

    $ git-commit   

    ###########################################################################
    # If you are not using the convenience scripts, use the following commands:
    ###########################################################################

    $ git commit -a

You can do as many commits as you want prior to pushing your changes to github.

### Push your changes to github

Verify you are in the correct local branch:

    $ git branch
    <Verify the branch name>
    
Then rename the branch based upon the content of your changes and then push the branch to the remote repository: 

    $ git branch -m <my-awesome-feature-branch-name>
    $ git-push

    ###########################################################################
    # If you are not using the convenience scripts, use the following commands:
    ###########################################################################

    $ git branch -m <my-awesome-feature-branch-name>
    $ git push origin <my-awesome-feature-branch-name>

### Submit a pull request for your branch

* Go to the [karma exchange project](https://github.com/karma-exchange-org/karma-exchange.git) on github.
* Click "Branches"
* Select your branch
* Click "Pull Request" to submit a pull request.

### Reviewing the code

The code should ideally be reviewed by someone else in the organization. See the [git hub pull tutorial](https://help.github.com/articles/using-pull-requests#managing-pull-requests) to see how pull requests are processed by the code reviewer.

To incorporate code review feedback just follow the prior modification and push instructions and re-run your tests, but skip the step of creating a new branch and renaming the branch:

    <make your changes>
    $ mvn appengine:devserver
    $ git-commit
    $ git-push

### Merging the code

Once you have received code review approval, open the pull request page on github and click the "Merge pull request" button. After the code is merged, delete your branch on git hub by clicking "delete branch" button.

If you have conflicts that prevent you from merging you'll need to update your local master and and merge your feature branch to it:

    $ git checkout master
    $ git pull
    <this will update your local master>
    $ git checkout <my-awesome-feature-branch-name>
    $ git merge master
    $ git status
    $ meld .
    
Resolve any conflicts using meld or your editor and test out your changes. Then commit your changes and push them to github. Then you should be able to merge the branch using the github UI.
    
    <test out changes to make sure everything still works>
    $ git-commit
    $ git-push
    <go to the github pull request page and re-attempt "Merge pull request">


### Cleanup your local branch

Sync your master branch to the latest changes and delete your merged branch:

    $ git-cleanup

    ###########################################################################
    # If you are not using the convenience scripts, use the following commands:
    ###########################################################################

    $ git checkout master
    $ git remote update --prune
    $ git pull
    $ git branch -d <my-awesome-feature-branch-name>


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

### Convenience scripts / aliases

I've created a few convenience scripts that I recommend you download [from here](https://www.dropbox.com/sh/qbeli6omtrbdoyu/oG4QNpe79L)

    git-cleanup.sh
    git-commit.sh
    git-push.sh
    git-merge.sh

Once you download them put them in your preferred directory (example below uses ~/bin), chmod them to make them executable ('chmod 744 git*.sh') and create aliases to them:

    $ cat ~/.bash_aliases
    alias git-cleanup=~/bin/git-cleanup.sh
    alias git-commit=~/bin/git-commit.sh
    alias git-diff='git difftool master --dir-diff'
    alias git-merge=~/bin/git-merge.sh
    alias git-push=~/bin/git-push.sh

Note that 'git-diff' works really well if you have meld setup.

<a name="links"/>
## Helpful Git Links

1. Github:
   * https://help.github.com/articles/set-up-git
   * [Merging a pull request and dealing with merge conflcits](https://help.github.com/articles/merging-a-pull-request). 
2. Github collaboration. This is our template for collaboration:
   * https://gist.github.com/seshness/3943237
3. Git:
   * http://git-scm.com/documentation
   * http://www.vogella.com/articles/Git/article.html
   * http://gitref.org/basic/
