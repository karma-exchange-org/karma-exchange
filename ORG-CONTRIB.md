# Karma Exchange Organization Contributors

## Uploading changes

Our method for collobaration is based upon this wiki: https://gist.github.com/seshness/3943237

### Checkout a branch for your feature before doing anything:

    $ git checkout -b <my-awesome-feature>

..work on things in your favourite text $EDITOR..

    $ git add [files]
    $ git commit
    or
    $ git commit -a

### To push it to the remote repository as a non-master branch, run

    $ git push origin <my-awesome-feature>

### Submit a pull request

* Go to github for the [Karma Exchange Project](https://github.com/karma-exchange-org/karma-exchange.git)

* click "Branches"
* select your branch
* Click "pull request" to submit a pull request.


### Reviewing the code

The code should ideally be reviewed by someone else in the organization. See the [git hub pull tutorial](https://help.github.com/articles/using-pull-requests#managing-pull-requests) to see how pull requests are processed by the code reviewer.

### Merging the code

Once you have received code review approval follow the [merge instructions](https://help.github.com/articles/merging-a-pull-request). In most cases it should just be as simple as clicking the "Merge pull request" button.

After the code is merged, delete your branch on git hub. Click "delete branch".

### Cleanup your local branch

    git checkout master
    git branch -d <my-awesome-feature>
    git remote update --prune

Sync your master branch to the latest changes

    git pull


## Setup

### Setting up git on your computer

https://help.github.com/articles/set-up-git

    git config --global user.name "First Last"
    git config --global user.email "see.fake.email.notes@gmail.com"
    git config --global core.editor emacs
    git config --global credential.helper cache
    git config --global credential.helper 'cache --timeout=360000'

### Getting a local copy of the repository

Assuming that you're placing the repository in ~/src

    cd ~/
    mkdir src
    cd ~/src
    git clone https://github.com/karma-exchange-org/karma-exchange.git
    cd karma-exchange
    git config --list

## Helpful Git Links

1. Github:
   * https://help.github.com/articles/set-up-git

2. Github collaboration. This is our template for collaboration:
   * https://gist.github.com/seshness/3943237

3. Git:
   * http://www.vogella.com/articles/Git/article.html
   * http://gitref.org/basic/
