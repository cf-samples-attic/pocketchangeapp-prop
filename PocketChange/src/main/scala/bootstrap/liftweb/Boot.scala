package bootstrap.liftweb

import net.liftweb._
import common.{MDC, Full, Logger}
import util.Props
import http.{LiftRules, ParsePath, Req, RewriteRequest, RewriteResponse, S}
import sitemap.{Loc, Menu, SiteMap}
import mapper._

// Get implicit conversions

import util.Helpers._

import com.pocketchangeapp.snippet.{AddEntry, Accounts}
import com.pocketchangeapp.model._
import com.pocketchangeapp.api._
import com.pocketchangeapp.util.{Charting, Image}

/**
 * The bootstrap.liftweb.Boot class is the main entry point for Lift.
 * This is where all of the initial App setup is performed. In particular,
 * the Boot.boot method is what lift calls after it loads.
 *
 * TODO: Connect Lucene/Compass for search
 */
class Boot {
  // Set up a logger to use for startup messages
  val logger = Logger(classOf[Boot])

  def boot {
    /*
     * LiftRules.early allows us to apply functions to the request before
     * Lift has started to work with it. In our case, we're explicitly
     * setting the character encoding to work around an issue with
     * Jetty not properly discovering the encoding from the client.
     */
    LiftRules.early.append {
      _.setCharacterEncoding("UTF-8")
    }

    /*
     * If you're using reflection-based dispatch in Lift, then you need to
     * tell Lift which packages contain the View/Snippet/Comet classes.
     */
    LiftRules.addToPackages("com.pocketchangeapp")

    if (!DB.jndiJdbcConnAvailable_?) DB.defineConnectionManager(DefaultConnectionIdentifier, DBVendor)

    Schemifier.schemify(true, Schemifier.infoF _,
      User, Tag, Account, AccountAdmin,
      AccountViewer, AccountNote, Expense, ExpenseTag)

    LiftRules.setSiteMap(SiteMap(MenuInfo.menu: _*))

    // Tie in the REST API. Uncomment the one you want to use
    //LiftRules.dispatch.prepend(DispatchRestAPI.dispatch)
    LiftRules.dispatch.prepend(RestHelperAPI)

    // We use static dispatch to resolve our snippets to avoid accidental exposure via reflection
    LiftRules.snippetDispatch.append {
      case "Accounts" => Accounts
      case "AddEntry" => new AddEntry
    }

    // Set up some rewrites
    LiftRules.statelessRewrite.append {
      case RewriteRequest(ParsePath(List("account", acctName), _, _, _), _, _) =>
        RewriteResponse("viewAcct" :: Nil, Map("name" -> urlDecode(acctName)))
      case RewriteRequest(ParsePath(List("account", acctName, tag), _, _, _), _, _) =>
        RewriteResponse("viewAcct" :: Nil, Map("name" -> urlDecode(acctName), "tag" -> urlDecode(tag)))
    }

    // Custom dispatch for graph and receipt image generation
    LiftRules.dispatch.append {
      case Req(List("graph", acctName, "history"), _, _) =>
        () => Charting.history(acctName)
      case Req(List("graph", acctName, "tagpie"), _, _) =>
        () => Charting.tagpie(acctName)
      case Req(List("graph", acctName, "tagbar"), _, _) =>
        () => Charting.tagbar(acctName)
      case Req(List("image", expenseId), _, _) =>
        () => Full(Image.viewImage(expenseId))
    }

    // Hook in our REST API auth
    LiftRules.httpAuthProtectedResource.append(DispatchRestAPI.protection)

    /* We're going to use HTTP Basic auth for REST, although
     * technically this allows for its use anywhere in the app. */
    import net.liftweb.http.auth.{AuthRole, HttpBasicAuthentication, userRoles}
    LiftRules.authentication = HttpBasicAuthentication("PocketChange") {
      case (userEmail, userPass, _) => {
        logger.debug("Authenticating: " + userEmail)
        User.find(By(User.email, userEmail)).map {
          user =>
            if (user.password.match_?(userPass)) {
              logger.info("Auth succeeded for " + userEmail)
              User.logUserIn(user)

              // Set an MDC for logging purposes
              MDC.put("user" -> user.shortName)

              // Compute all of the user roles
              userRoles(user.editable.map(acct => AuthRole("editAcct:" + acct.id)) ++
                user.allAccounts.map(acct => AuthRole("viewAcct:" + acct.id)))
              true
            } else {
              logger.warn("Auth failed for " + userEmail)
              false
            }
        } openOr false
      }
    }

    //    // Add a query logger (directly)
    //    DB.addLogFunc {
    //      case (log, duration) => {
    //        logger.debug("Total query time : %d ms".format(duration))
    //        log.allEntries.foreach {
    //          case DBLogEntry(stmt,duration) =>
    //            logger.debug("  %s in %d ms".format(stmt, duration))
    //        }
    //      }
    //    }

    // Add a query logger (via S.queryLog)
    DB.addLogFunc(DB.queryCollector)

    S.addAnalyzer {
      case (Full(req), duration, log) => {
        logger.debug(("Total request time on %s: %d ms").format(req.uri, duration))
        log.foreach {
          case (stmt, dur) =>
            logger.debug("  %s in %d ms".format(stmt, dur))
        }
      }
      case _ => // we don't log for non-requests
    }

    logger.info("Bootstrap up")
  }
}

object MenuInfo {

  import Loc._

  // Define a simple test clause that we can use for multiple menu items
  val IfLoggedIn = If(() => User.currentUser.isDefined, "You must be logged in")

  def menu: List[Menu] =
    List[Menu](Menu.i("Home") / "index",
      Menu.i("Manage Accounts") / "manage" >> IfLoggedIn,
      Menu.i("Add Account") / "editAcct" >> Hidden >> IfLoggedIn,
      Menu.i("View Account") / "viewAcct" / sitemap.** >> Hidden >> IfLoggedIn,
      Menu.i("Help") / "help" / "index") :::
      User.sitemap

}

object DBVendor extends StandardDBVendor(
  Props.get("db.class").openOr("Invalid DB class"),
  Props.get("db.url").openOr("Invalid DB url"),
  Props.get("db.user"),
  Props.get("db.pass"))
