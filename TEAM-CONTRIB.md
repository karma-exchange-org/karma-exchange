# Instructions for members of the Karma Exchange product team to contribute changes

## Uploading changes

Our method for collobaration is based upon this wiki: https://gist.github.com/seshness/3943237

### Make and test your changes

Make whatever changes you want to make. Make sure all tests pass:

    $ mvn test

### Create a branch with your changes

Create a branch using "git checkout -b". Don't worry, this command automatically copies your changes to the new branch:

    $ cd <project git dir>
    $ git checkout -b <my-awesome-feature>

Add any new files:

    $ git add .

Review your changes:

    $ git status

Commit your changes:

    $ git commit -a    

(Optional) Review your commits:

    $ gitk

Push your branch to the remote repository as a non-master branch:

    $ git push origin <my-awesome-feature>

### Submit a pull request for your branch

* Go to the [karma exchange project](https://github.com/karma-exchange-org/karma-exchange.git) on github.
* Click "Branches"
* Select your branch
* Click "Pull Request" to submit a pull request.

### Reviewing the code

The code should ideally be reviewed by someone else in the organization. See the [git hub pull tutorial](https://help.github.com/articles/using-pull-requests#managing-pull-requests) to see how pull requests are processed by the code reviewer.

To incorporate code review feedback just follow the prior modification and push instructions and re-run your tests, but skip the step of creating a new branch:

    <make your changes>
    $ mvn test
    $ git add .
    $ git commit -a    
    $ git push origin <my-awesome-feature>

### Merging the code

Once you have received code review approval follow the [merge instructions](https://help.github.com/articles/merging-a-pull-request). In most cases it should just be as simple as clicking the "Merge pull request" button.

After the code is merged, delete your branch on git hub by clicking "delete branch".

### Cleanup your local branch

Sync your master branch to the latest changes:

    git checkout master
    git remote update --prune
    git pull

Delete your merged branch:

    git branch -d <my-awesome-feature>

## Setup

### Setting up git on your computer

Read https://help.github.com/articles/set-up-git

    git config --global user.name "First Last"
    git config --global user.email "fake.email@gmail.com"
    git config --global core.editor $EDITOR
    git config --global credential.helper cache
    git config --global credential.helper 'cache --timeout=360000'

Note: make sure to register your fake email in github.

### Getting a local copy of the repository

Assuming that you're placing the repository in ~/src

    mkdir ~/src
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
