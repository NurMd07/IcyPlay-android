const popupWrapper = document.querySelector("#popup-wrapper");
const popupWrapper1 = document.querySelector("#popup-wrapper1");
const popupWrapper2 = document.querySelector("#popup-wrapper2");
const addNewBtn = document.querySelector(".addnewbtn");
const popupCancel = document.querySelector(".popup-cancel");
const popupCancel1 = document.querySelector(".popup-cancel1");
const popupSubmit = document.querySelector(".popup-submit");
const popupSubmit1 = document.querySelector(".popup-submit1");
const addTags = document.querySelector(".addtags");
const addTags1 = document.querySelector(".addtags1");
const theme = document.querySelector(".theme");
const theme1 = document.querySelector(".theme1");
const form = document.querySelector("#form");
const form1 = document.querySelector("#form1");
const logoUrl = document.querySelector(".logourl");
const logoUrl1 = document.querySelector(".logourl1");
const menuOptions = document.querySelector(".menu-options");
const removeConfirmGroup = document.querySelector('.remove-confirm-group');
const removeConfirmNo = document.querySelector('.remove-confirm-no');
const removeConfirmYes = document.querySelector('.remove-confirm-yes');
const gamesContainer = document.querySelector(".games-container");
const heading = document.querySelector(".heading");

let gameName = document.querySelector(".gamename");
let userId = document.querySelector(".userid");
let password = document.querySelector(".password");
let gameName1 = document.querySelector(".gamename1");
let userId1 = document.querySelector(".userid1");
let password1 = document.querySelector(".password1");
let tagsInput = document.querySelector(".tags");
let tagsDiv = document.querySelector(".tagsdiv");
let tagsInput1 = document.querySelector(".tags1");
let tagsDiv1 = document.querySelector(".tagsdiv1");
let removeName = document.querySelector('.remove-name');
let editId = document.querySelector('#editid');
const loader = document.querySelector('.lds-facebook');
const loader1 = document.querySelector('.lds-facebook1');
const body = document.body;

let allGames = [];
let allTags = [];

class GameInfo {
  constructor(gameName, userId, password, theme, logoUrl, allTags) {
    this.gameName = gameName;
    this.userId = userId;
    this.password = password;
    this.theme = theme;
    this.logoUrl = logoUrl;
    this.allTags = allTags || [];
  }
}

const savedTheme = localStorage.getItem('theme');
const systemPrefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;

if (savedTheme) {
  body.setAttribute('data-theme', savedTheme);
  Android.setTheme(savedTheme);
} else if (systemPrefersDark) {
  body.setAttribute('data-theme', 'dark');
  Android.setTheme('dark');
} else {
  body.setAttribute('data-theme', 'light ');
  Android.setTheme('light');
}

const toggleButton = document.getElementById('theme-toggle');
toggleButton.addEventListener('click', () => {
  let theme = body.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
  body.setAttribute('data-theme', theme);

  Android.setTheme(theme);
  localStorage.setItem('theme', theme);
});



function tagsPlaceholder(div) {
  const span = document.createElement("span");
  span.classList.add("tagsplaceholder");
  span.textContent = `No Tags added`;
  if (!div.querySelector('.tagsplaceholder')) {
    div.appendChild(span);
  }

}
function clearform() {
  gameName.value = "";
  userId.value = "";
  password.value = "";
  allTags = [];
  tagsInput.value = "";
  tagsDiv.innerHTML = "";
  logoUrl.value = "";
  tagsPlaceholder(tagsDiv);
  theme.value = "#595959";
}
function clearform1() {
  gameName1.value = "";
  userId1.value = "";
  password1.value = "";
  allTags = [];
  tagsInput1.value = "";
  tagsDiv1.innerHTML = "";
  logoUrl1.value = "";
  tagsPlaceholder(tagsDiv1);
}

function toggleMenu() {
  const themeToggle = document.querySelector('#theme-toggle');
  const backupToggle = document.querySelector('.backup');
  const restoreToggle = document.querySelector('.restore');

  themeToggle.classList.toggle('show');
  backupToggle.classList.toggle('hide');
  restoreToggle.classList.toggle('hide');
  addNewBtn.classList.toggle('hide');
}

addNewBtn.addEventListener("click", () => {
  clearform();
  let currentTheme = body.getAttribute('data-theme') === 'dark' ? 'dark' : 'light';
  if (currentTheme === "light") {
    theme.value = "#595959";
  } else {
    theme.value = "#d9d9d9";
  }
  popupWrapper.classList.add("show");
});

popupCancel.addEventListener("click", () => {
  clearform();
  popupWrapper.classList.remove("show");
});
popupCancel1.addEventListener("click", () => {
  clearform1();
  popupWrapper2.classList.remove("show");
});



addTags.addEventListener("click", () => {
  if (tagsInput.value) {
    if (allTags.length <= 0) {
      tagsDiv.innerHTML = "";
    }
    allTags.push(tagsInput.value);
    createTag(tagsInput.value, tagsDiv);
  }
  tagsInput.value = "";
});

addTags1.addEventListener("click", () => {

  if (tagsInput1.value) {
    if (allTags.length <= 0) {
      tagsDiv1.innerHTML = "";
    }
    allTags.push(tagsInput1.value);
    createTag(tagsInput1.value, tagsDiv1);
  }
  tagsInput1.value = "";
});

tagsDiv.addEventListener("click", function (event) {

  if (
    event.target.tagName.toLowerCase() === "span" &&
    !event.target.classList.contains("tagsplaceholder")
  ) {
    let index = allTags.indexOf(event.target.textContent);
    if (index !== -1) allTags.splice(index, 1);
    event.target.remove();
  }
  if (allTags.length <= 0) {
    tagsPlaceholder(tagsDiv);
  }
});

tagsDiv1.addEventListener("click", function (event) {

  if (
    event.target.tagName.toLowerCase() === "span" &&
    !event.target.classList.contains("tagsplaceholder")
  ) {
    let index = allTags.indexOf(event.target.textContent);
    if (index !== -1) allTags.splice(index, 1);
    event.target.remove();

  }

  if (allTags.length <= 0) {
    tagsPlaceholder(tagsDiv1);
  }
});

function createSVG(width, height, fill, className, viewBox, paths) {
  const svgNS = "http://www.w3.org/2000/svg";


  const svg = document.createElementNS(svgNS, "svg");
  svg.setAttribute("width", width);
  svg.setAttribute("height", height);
  svg.setAttribute("fill", fill);
  svg.setAttribute("class", className);
  svg.setAttribute("viewBox", viewBox);


  paths.forEach(({ d, fillRule }) => {
    const path = document.createElementNS(svgNS, "path");
    path.setAttribute("d", d);
    if (fillRule) {
      path.setAttribute("fill-rule", fillRule);
    }
    svg.appendChild(path);
  });

  return svg;
}

function eventOnInfo(code) {
  let span = document.createElement("span");
  if (code.classList.contains("id")) {
    span.textContent = "🆔";
    span.classList.add('emoji-id')
  } else {
    span.textContent = "🔒";
    span.classList.add('emoji-password')
  }
  span.style.position = "absolute";
  span.style.left = "1em";
  code.appendChild(span);
  code.addEventListener("click", function (event) {

    const svgElement = document.createElementNS(
      "http://www.w3.org/2000/svg",
      "svg"
    );
    svgElement.setAttribute("xmlns", "http://www.w3.org/2000/svg");
    svgElement.setAttribute("width", "16");
    svgElement.setAttribute("height", "16");
    svgElement.setAttribute("fill", "green");
    svgElement.setAttribute("class", "bi bi-clipboard-check");
    svgElement.setAttribute("viewBox", "0 0 16 16");


    const path1 = document.createElementNS(
      "http://www.w3.org/2000/svg",
      "path"
    );
    path1.setAttribute("fill-rule", "evenodd");
    path1.setAttribute(
      "d",
      "M10.854 7.146a.5.5 0 0 1 0 .708l-3 3a.5.5 0 0 1-.708 0l-1.5-1.5a.5.5 0 1 1 .708-.708L7.5 9.793l2.646-2.647a.5.5 0 0 1 .708 0"
    );
    svgElement.appendChild(path1);


    const path2 = document.createElementNS(
      "http://www.w3.org/2000/svg",
      "path"
    );
    path2.setAttribute(
      "d",
      "M4 1.5H3a2 2 0 0 0-2 2V14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V3.5a2 2 0 0 0-2-2h-1v1h1a1 1 0 0 1 1 1V14a1 1 0 0 1-1 1H3a1 1 0 0 1-1-1V3.5a1 1 0 0 1 1-1h1z"
    );
    svgElement.appendChild(path2);

    const path3 = document.createElementNS(
      "http://www.w3.org/2000/svg",
      "path"
    );
    path3.setAttribute(
      "d",
      "M9.5 1a.5.5 0 0 1 .5.5v1a.5.5 0 0 1-.5.5h-3a.5.5 0 0 1-.5-.5v-1a.5.5 0 0 1 .5-.5zm-3-1A1.5 1.5 0 0 0 5 1.5v1A1.5 1.5 0 0 0 6.5 4h3A1.5 1.5 0 0 0 11 2.5v-1A1.5 1.5 0 0 0 9.5 0z"
    );
    svgElement.appendChild(path3);

    svgElement.style.position = "absolute";
    svgElement.style.right = "1em";

    code.appendChild(svgElement);
    code.style.color = "gray";

    const theme = localStorage.getItem('theme');
    window.setTimeout(() => {
      code.removeChild(svgElement);

      if (theme === "dark") {
        code.style.color = "#f5f5f5";
      } else {
        code.style.color = "#0c0c0c";
      }
    }, 5000);
    let TempText = document.createElement("input");

    TempText.value = code.textContent.replace("🆔", "").replace("🔒", "");

    document.body.appendChild(TempText);
    TempText.select();

    document.execCommand("copy");
    document.body.removeChild(TempText);
  });
}

function createGameDiv(game) {
  const gameDiv = document.createElement("div");
  gameDiv.classList.add("game");

  gameDiv.style = `box-shadow:0px 0px 10px -5px ${game.theme};`;

  const pin = document.createElement("div");
  pin.classList.add("pin");
  pin.innerHTML = `<img height="12px" width="12px" src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABkAAAAZCAYAAADE6YVjAAAACXBIWXMAAAsTAAALEwEAmpwYAAABXklEQVR4nO3TvUrDUBQH8IBOTiLo4uAXfky1zTm13bqIb9AXUJBiz0nsE+joQzg4OziIJOekSukkCAoOTnZzKohPYK1caEtpbQhp49Q/HLg3ubm/hHtiWdP8R4BkB0kbyBrsurX5hBA9Qta2KSB9SgSC49s5IH1LDCoWr2eA9RxJW12kU8/5iiyMDaRKugSs1YHN40M5J9hE8vdzrr9q5maMLM0QoFtBJABJrvofBJIXYP0evbH89M6ItBER0dcIb9zuFemFaQZkOcyWq1uRECj7NrJ+RQGApF44q81acZJ1NQ0sn+GINPOV++VYQCSItAWsB9YkAmW/NKpdJwKg42WB9KMDDZ1Rt71jJ+N4KWR9N5CZ247CMBQU4n+B62+bNjZQ/3V7EDqtrscC9kjXkOTRQH/dtx0F83Miy2UswLQjsjwAextWEkmztwgkN2P3fVgyJ3crBgpdNI2VcH4BeGFJpcsORokAAAAASUVORK5CYII=">`
  gameDiv.appendChild(pin);

  if (game.isPinned === "true") {
    pin.classList.add("pinShow");
  } else {
    pin.classList.remove("pinShow");
  }
  const pinMenu = document.createElement("div");
  pinMenu.classList.add("pinMenu");

  const pinInput = document.createElement('input');
  pinInput.type = 'checkbox';
  pinInput.dataset.id = game._id;
  pinInput.checked = game.isPinned === "true" ? true : false;
  pinInput.classList.add('pinInput');
  pinInput.addEventListener("change", function () {
    if (this.checked) {

      let date = (new Date()).toISOString();
      Android.updatePinnedStatusById(this.dataset.id, true, date);
      fetchAndDisplayGames()
    } else {
      let date = (new Date()).toISOString();
      Android.updatePinnedStatusById(this.dataset.id, false, date);
      fetchAndDisplayGames()
    }
    toggleMenu();
  });

  const pinLabel = document.createElement('label');
  pinLabel.textContent = "Pin";
  pinLabel.setAttribute('for', 'pinInput');
  pinLabel.classList.add('pinLabel')
  const pinSvgPath = [
    {
      d: "M4.146.146A.5.5 0 0 1 4.5 0h7a.5.5 0 0 1 .5.5c0 .68-.342 1.174-.646 1.479-.126.125-.25.224-.354.298v4.431l.078.048c.203.127.476.314.751.555C12.36 7.775 13 8.527 13 9.5a.5.5 0 0 1-.5.5h-4v4.5c0 .276-.224 1.5-.5 1.5s-.5-1.224-.5-1.5V10h-4a.5.5 0 0 1-.5-.5c0-.973.64-1.725 1.17-2.189A6 6 0 0 1 5 6.708V2.277a3 3 0 0 1-.354-.298C4.342 1.674 4 1.179 4 .5a.5.5 0 0 1 .146-.354",
      fill: "currentColor"
    }
  ];

  const pinSvg = createSVG("10", "10", "currentColor", "bi bi-pin-fill", "0 0 16 16", pinSvgPath);
  pinSvg.style.marginLeft = "3px";
  pinSvg.style.marginTop = "0px";

  pinLabel.appendChild(pinSvg)
  pinMenu.appendChild(pinLabel);

  pinMenu.appendChild(pinInput);
  gameDiv.appendChild(pinMenu);

  const gameTitleDiv = document.createElement("div");
  gameTitleDiv.classList.add("gametitle");

  const img = document.createElement("img");
  img.classList.add("gameimg");
  img.src = game.logoUrl;
  img.onerror = function () {
    if (!this.hasTriedPlaceholder) {
      this.hasTriedPlaceholder = true;
      this.src = 'images/placeholder-image.png';  // Set the placeholder image
    } else {
      // Optionally, remove the event handler if the placeholder also fails
      this.onerror = null;  // Prevent further errors to stop the loop
    }
  };
  gameTitleDiv.appendChild(img);

  const gameTitle = document.createElement('h3');
  gameTitle.textContent = game.gameName;
  gameTitle.style.color = game.theme;
  gameTitleDiv.appendChild(gameTitle);

  gameDiv.appendChild(gameTitleDiv);

  const id = document.createElement("code");
  id.classList.add("id");
  id.textContent = game.userId;
  eventOnInfo(id);
  gameDiv.appendChild(id)


  const pass = document.createElement("code");
  pass.classList.add("pass");
  pass.textContent = game.password;
  eventOnInfo(pass);
  gameDiv.appendChild(pass)

  const tagsDiv = document.createElement('div');
  tagsDiv.classList.add('showtagsdiv');
  const tags = game.tags.split(',');
  tags.forEach((tag) => {
    const span = document.createElement("span");
    span.classList.add("showtag");
    span.textContent = `${tag}`;
    tagsDiv.appendChild(span);
  })
  gameDiv.appendChild(tagsDiv);

  const options = document.createElement('div');
  options.classList.add('options');

  const optionEdit = document.createElement('span');
  optionEdit.classList.add('option-edit');
  optionEdit.textContent = 'Edit';
  optionEdit.dataset.id = game._id;

  const path1 = [
    { d: "M15.502 1.94a.5.5 0 0 1 0 .706L14.459 3.69l-2-2L13.502.646a.5.5 0 0 1 .707 0l1.293 1.293zm-1.75 2.456-2-2L4.939 9.21a.5.5 0 0 0-.121.196l-.805 2.414a.25.25 0 0 0 .316.316l2.414-.805a.5.5 0 0 0 .196-.12l6.813-6.814z" },
    { d: "M1 13.5A1.5 1.5 0 0 0 2.5 15h11a1.5 1.5 0 0 0 1.5-1.5v-6a.5.5 0 0 0-1 0v6a.5.5 0 0 1-.5.5h-11a.5.5 0 0 1-.5-.5v-11a.5.5 0 0 1 .5-.5H9a.5.5 0 0 0 0-1H2.5A1.5 1.5 0 0 0 1 2.5z", fillRule: "evenodd" }
  ];


  const svgElement1 = createSVG("13", "13", "currentColor", "bi bi-pencil-square", "0 0 16 16", path1);

  optionEdit.appendChild(svgElement1)

  options.appendChild(optionEdit);

  const optionRemove = document.createElement('span');
  optionRemove.classList.add('option-remove');
  optionRemove.textContent = 'Delete';
  optionRemove.dataset.id = game._id;
  optionRemove.dataset.gamename = game.gameName;
  optionRemove.dataset.theme = game.theme;
  optionRemove.dataset.logourl = game.logoUrl;

  const path2 = [
    { d: "M6.5 1h3a.5.5 0 0 1 .5.5v1H6v-1a.5.5 0 0 1 .5-.5M11 2.5v-1A1.5 1.5 0 0 0 9.5 0h-3A1.5 1.5 0 0 0 5 1.5v1H1.5a.5.5 0 0 0 0 1h.538l.853 10.66A2 2 0 0 0 4.885 16h6.23a2 2 0 0 0 1.994-1.84l.853-10.66h.538a.5.5 0 0 0 0-1zm1.958 1-.846 10.58a1 1 0 0 1-.997.92h-6.23a1 1 0 0 1-.997-.92L3.042 3.5zm-7.487 1a.5.5 0 0 1 .528.47l.5 8.5a.5.5 0 0 1-.998.06L5 5.03a.5.5 0 0 1 .47-.53Zm5.058 0a.5.5 0 0 1 .47.53l-.5 8.5a.5.5 0 1 1-.998-.06l.5-8.5a.5.5 0 0 1 .528-.47M8 4.5a.5.5 0 0 1 .5.5v8.5a.5.5 0 0 1-1 0V5a.5.5 0 0 1 .5-.5" }
  ]

  const svgElement2 = createSVG("13", "13", "currentColor", "bi bi-trash3", "0 0 16 16", path2);

  optionRemove.appendChild(svgElement2)

  options.appendChild(optionRemove);

  gameDiv.appendChild(options);



  return gameDiv;
}

function createGamePlaceholder() {
  const gameDiv = document.createElement('div');
  gameDiv.classList.add('gameplaceholder');

  gameDiv.innerHTML = '';


  let text1 = document.createElement('span');
  text1.innerHTML = 'Nothing to <b> Play ? </b>'

  text1.style.marginBottom = "0.7em"
  let text2 = document.createElement('span');
  text2.innerHTML = `Create a new game to get started !`;

  gameDiv.appendChild(text1);
  gameDiv.appendChild(text2);

  gamesContainer.appendChild(gameDiv);
}

function fetchAndDisplayGames() {

  gamesContainer.innerHTML = "";

  // Call the Android interface to get all game info
  const gameInfo = Android.getAllGameInfo();

  try {

    const allGames = JSON.parse(gameInfo);
    if (allGames.length == 0) {
      createGamePlaceholder();
      return;
    }

    allGames.forEach((game) => {

      const theme = localStorage.getItem('theme');

      if (game.theme === '#595959' || game.theme === '#d9d9d9') {
        if (theme === 'dark') {
          game.theme = '#d9d9d9';
        } else {
          game.theme = '#595959';
        }
      }

      const gameDiv = createGameDiv(game);
      gamesContainer.appendChild(gameDiv);
    });

  } catch (error) {

    console.log("Error parsing game info: " + error.message);
  }
}

const backup = document.querySelector('.backup');
const restore = document.querySelector('.restore');

backup.addEventListener('click', () => {
  Android.exportData();
})

restore.addEventListener('click', () => {
  Android.importData();
});

window.onDataImported = function () {
  Android.refresh();
};

window.onload = fetchAndDisplayGames();

let base64Image = "";
let localLogoPicked = false;

form.addEventListener("submit", function (event) {
  event.preventDefault();

  popupSubmit.classList.add('hide');
  loader.classList.remove('hide');

  let game = new GameInfo(
    gameName.value,
    userId.value,
    password.value,
    theme.value,
    logoUrl.value,
    allTags
  );

  if (gameName.value && userId.value && password.value) {

    const tags = allTags.join(",");
    const currentTime = (new Date()).toISOString();

    if (game.logoUrl.startsWith("http://") || game.logoUrl.startsWith("https://")) {
      localLogoPicked = false
    } else {
      localLogoPicked = true

    }

    setTimeout(function () {
      Android.saveGameInfo(
        game.gameName,
        game.userId,
        game.password,
        game.theme,
        game.logoUrl,
        tags,
        false,
        currentTime,
        localLogoPicked,
        base64Image
      );
      window.scrollTo(0, 0);

    }, 100);
  }
});

// This function will be called from Android once the save is complete
window.onGameInfoSaved = function () {
  fetchAndDisplayGames();
  popupWrapper.classList.remove("show");
  clearform();

  popupSubmit.classList.remove('hide');
  loader.classList.add('hide');
};


function fetchAndEditGame(gameId) {

  // Call the Android interface to get the game info
  const gameInfo = Android.getGameInfoById(gameId);

  try {
    const game = JSON.parse(gameInfo);

    gameName1.value = game.gameName;
    userId1.value = game.userId;
    password1.value = game.password;
    theme1.value = game.theme;
    logoUrl1.value = game.remoteLogoUrl;
    editId.value = game._id;
    allTags = game.tags ? game.tags.split(",") : [];

    if (allTags.length) {
      tagsDiv1.innerHTML = "";
      allTags.forEach(tag => {
        createTag(tag, tagsDiv1);
      })

    }
  } catch (error) {
    console.log("Error parsing game info: " + error.message);
  }
}

menuOptions.addEventListener("click", () => {

  const optionEdit = document.querySelectorAll(".option-edit");
  const optionRemove = document.querySelectorAll(".option-remove");
  const options = document.querySelectorAll(".options");
  const pinMenu = document.querySelectorAll('.pinMenu');
  const themeToggle = document.querySelector('#theme-toggle');
  const backupToggle = document.querySelector('.backup');
  const restoreToggle = document.querySelector('.restore');

  themeToggle.classList.toggle('show');
  backupToggle.classList.toggle('hide');
  restoreToggle.classList.toggle('hide');
  addNewBtn.classList.toggle('hide');

  pinMenu.forEach((pin) => {
    pin.classList.toggle('pinShow');
  })
  optionEdit.forEach((option) => {
    option.addEventListener("click", () => {
      clearform1();
      fetchAndEditGame(option.dataset.id);
      popupWrapper2.classList.add('show');

    });
  })

  optionRemove.forEach((option) => {
    option.addEventListener("click", () => {
      removeName.textContent = option.dataset.gamename;
      removeName.style.color = option.dataset.theme;
      removeConfirmYes.dataset.id = option.dataset.id;
      removeConfirmYes.dataset.logourl = option.dataset.logourl;
      popupWrapper1.classList.add('show');
    });
  })

  options.forEach((option) => {
    option.classList.toggle("options-show");
  })

});

form1.addEventListener("submit", function (event) {

  event.preventDefault();
  toggleMenu();
  popupSubmit1.classList.add('hide');
  loader1.classList.remove('hide');

  let game = new GameInfo(
    gameName1.value,
    userId1.value,
    password1.value,
    theme1.value,
    logoUrl1.value,
    allTags
  );

  if (gameName1.value && userId1.value && password1.value) {
    let updatedTags = allTags.join(",");

    const isPinned = false;
    const modifiedAt = (new Date()).toISOString();
    if (game.logoUrl.startsWith("http://") || game.logoUrl.startsWith("https://")) {
      localLogoPicked = false
    } else {
      localLogoPicked = true

    }

    window.setTimeout(function () {
      Android.updateGameInfoById(
        editId.value,
        game.gameName,
        game.userId,
        game.password,
        game.theme,
        game.logoUrl,
        updatedTags,
        isPinned,
        modifiedAt,
        localLogoPicked,
        base64Image
      );

    }, 100)


  }
});

window.onGameInfoUpdated = function () {
  fetchAndDisplayGames();
  popupWrapper2.classList.remove("show");
  clearform1();
  popupSubmit1.classList.remove('hide');
  loader1.classList.add('hide');
};


removeConfirmNo.addEventListener('click', () => {
  popupWrapper1.classList.remove('show');
})

function deleteGameById(id, logoUrl) {
  Android.deleteGame(id, logoUrl);
}

removeConfirmYes.addEventListener('click', () => {
  toggleMenu();
  deleteGameById(removeConfirmYes.dataset.id, removeConfirmYes.dataset.logourl);
  popupWrapper1.classList.remove('show');
  fetchAndDisplayGames();
})

heading.addEventListener('click', () => {
  Android.refresh();
})

const selectLogoUrl = document.getElementById('selectlogourl');
const selectLogoUrl1 = document.getElementById('selectlogourl1');

function handleImagePick(base64Image1, fileName) {

  base64Image = `data:image/jpeg;base64,${base64Image1}`;

  logoUrl.value = fileName;
  logoUrl1.value = fileName;
}

selectLogoUrl.addEventListener('click', async () => {
  Android.openImagePicker();
});

selectLogoUrl1.addEventListener('click', async () => {
  Android.openImagePicker();
})