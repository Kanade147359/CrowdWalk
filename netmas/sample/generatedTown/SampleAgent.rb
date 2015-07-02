#! /usr/bin/env ruby
## -*- mode: ruby -*-
## = Sample Agent for CrowdWalk
## Author:: Itsuki Noda
## Version:: 0.0 2015/06/28 I.Noda
##
## === History
## * [2014/06/28]: Create This File.
## * [YYYY/MM/DD]: add more
## == Usage
## * ...

require 'RubyAgentBase.rb' ;

#--======================================================================
#++
## SampleAgent class
class SampleAgent < RubyAgentBase
  #--============================================================
  #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  #++
  ## Java から Ruby を呼び出すTriggerでのFilter。
  ## この配列に Java のメソッド名（キーワード）が入っていると、
  ## Ruby 側が呼び出される。入っていないと、無視される。
  ## RubyAgentBase を継承するクラスは、このFilterを持つことが望ましい。
  ## このFilterは、クラスをさかのぼってチェックされる。
  TriggerFilter = [
#                   "preUpdate",
#                   "update",
#                   "calcWayCostTo",
                   "thinkCycle",
                  ] ;

  #--------------------------------------------------------------
  #++
  ## シミュレーション各サイクルの前半に呼ばれる。
  ## _relTime_:: 相対シミュレーション時刻
  def preUpdate()
    p ['SampleAgent', :preUpdate, getAgentId(), currentTime()] ;
    return super()
  end

  #--------------------------------------------------------------
  #++
  ## シミュレーション各サイクルの後半に呼ばれる。
  ## _relTime_:: 相対シミュレーション時刻
  def update()
    p ['SampleAgent', :update, getAgentId(), currentTime()] ;
    return super() ;
  end

  #--------------------------------------------------------------
  #++
  ## あるwayを選択した場合の目的地(_target)までのコスト。
  ## _way_:: 現在進もうとしている道
  ## _node_:: 現在の分岐点
  ## _target_:: 最終目的地
  def calcWayCostTo(way, node, target)
    ## Term の中身の書き換えテスト。
    v = ItkTerm.getArg(@fallback, "xA_0").getDouble() + 1.0 ;
    ItkTerm.setArg(@fallback, "xA_0", v) ;

    ## 結果出力。
    pp ['SampleAgent', :calcWayCostTo, getAgentId(),
        way, node,
        ItkTerm.getHead(target),
        ItkTerm.toRuby(@fallback)] ;

    ## 元のものを呼び出す。
    return super(way, node, target) ;
  end

  #--------------------------------------------------------------
  #++
  ## 思考ルーチン
  def thinkCycle()

    if(listenAlert(Term_Emergency)) then
      setGoal(Term_node_09_05) ;
      clearRoute() ;
      p [:changeGoal, ItkTerm.toRuby(Term_node_09_05)] ;
      clearAlert(Term_Emergency) ;
    end

    if(listenAlert(Term_FooBarBaz)) then
      insertRoute(Term_node_02_00) ;
      p [:insertRoute, ItkTerm.toRuby(Term_node_02_00)] ;
      clearAlert(Term_FooBarBaz) ;
    end

  end

  Term_Emergency = ItkTerm.ensureTerm("emergency") ;
  Term_node_09_05 = ItkTerm.ensureTerm("node_09_05") ;
  Term_FooBarBaz = ItkTerm.ensureTerm("foo-bar-baz") ;
  Term_node_02_00 = ItkTerm.ensureTerm("node_02_00") ;

  #--------------------------------------------------------------
  #++
  ## 思考ルーチン (テスト用)
  def thinkCycle0()
    pp ['SampleAgent', :thinkCycle, getAgentId()] ;
    addAgentTag("hogehoge") ;
    pp [:agentTags, ItkTerm.toRuby(getAgentTags())] ;
    pp [:linkTags, ItkTerm.toRuby(getPlaceTags())] ;
    return super() ;
  end

end # class SampleAgent
